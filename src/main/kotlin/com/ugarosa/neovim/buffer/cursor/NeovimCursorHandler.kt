package com.ugarosa.neovim.buffer.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.common.FontSize
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.takeByte
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.setCursor
import com.ugarosa.neovim.rpc.event.handler.CursorMoveEvent
import com.ugarosa.neovim.rpc.type.NeovimPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext

class NeovimCursorHandler private constructor(
    private val bufferId: BufferId,
    private val editor: EditorEx,
    private val fontSize: FontSize,
    private val sendChan: SendChannel<suspend () -> Unit>,
) : Disposable {
    private val logger = myLogger()
    private val client = service<NeovimClient>()
    private val optionManager = service<NeovimOptionManager>()
    private val cursorListenerGuard =
        ListenerGuard(
            NeovimCursorListener(this),
            { editor.caretModel.addCaretListener(it) },
            { editor.caretModel.removeCaretListener(it) },
        )

    companion object {
        suspend fun create(
            bufferId: BufferId,
            editor: EditorEx,
            sendChan: SendChannel<suspend () -> Unit>,
        ): NeovimCursorHandler {
            val fontSize = FontSize.fromEditorEx(editor)
            val handler = NeovimCursorHandler(bufferId, editor, fontSize, sendChan)
            handler.cursorListenerGuard.register()
            return handler
        }
    }

    suspend fun handleCursorMoveEvent(event: CursorMoveEvent) =
        withContext(Dispatchers.EDT) {
            val originalOffset = editor.caretModel.offset
            val rawOffset = event.position.toOffset(editor.document)

            if (originalOffset == rawOffset) {
                logger.trace("No cursor move detected")
                return@withContext
            }

            logger.trace("Cursor move event: $event")

            val direction = determineDirection(originalOffset, rawOffset)

            val curswant = event.position.curswant
            val adjustedOffset = adjustOffsetForFoldedRegion(rawOffset, curswant, direction)
            val (scrolloff, sidescrolloff) = getScrollOptions()

            cursorListenerGuard.runWithoutListenerSuspend {
                // LogicalPosition does not strictly match the number of characters, such as counting a hard tab as
                // multiple characters. Since it is difficult to calculate the appropriate position considering this, a
                // simpler and more accurate offset is used instead.
                runUndoTransparentWriteAction {
                    editor.caretModel.moveToOffset(adjustedOffset)
                    val pos = editor.offsetToLogicalPosition(adjustedOffset)
                    scrollLineIntoView(pos.line, scrolloff)
                    scrollColumnIntoView(pos.column, sidescrolloff)
                }
            }

            if (adjustedOffset != rawOffset) {
                // If the offset was adjusted, we need to update the cursor position in Neovim
                // Restore the original curswant
                val offset = editor.caretModel.offset
                val position =
                    NeovimPosition.fromOffset(offset, editor.document)
                        .copy(curswant = curswant)
                syncIdeaToNeovim(position)
            }
        }

    enum class MoveDirection { UP, DOWN, LEFT, RIGHT }

    private fun determineDirection(
        fromOffset: Int,
        toOffset: Int,
    ): MoveDirection {
        val fromLine = editor.document.getLineNumber(fromOffset)
        val toLine = editor.document.getLineNumber(toOffset)
        return when {
            fromLine < toLine -> MoveDirection.DOWN
            fromLine > toLine -> MoveDirection.UP
            fromOffset < toOffset -> MoveDirection.RIGHT
            fromOffset > toOffset -> MoveDirection.LEFT

            else -> {
                // Same line, same offset
                MoveDirection.RIGHT
            }
        }
    }

    /**
     * Adjusts the document offset to account for folded regions when moving the cursor.
     *
     * Example:
     *
     * ```text
     * Before folding:
     * 5 | fun main() {
     * 6 |     println("Hello, World!")
     * 7 | }
     *
     * After folding:
     * 5 | fun main() { … }
     * 8 | // this line represents the line after the folded region
     * ```
     *
     * - Moving from line 5 to line 6 should land at line 8.
     * - Moving from line 8 to line 7 should land at line 5.
     */
    private fun adjustOffsetForFoldedRegion(
        offset: Int,
        curswant: Int,
        direction: MoveDirection,
    ): Int {
        if (getMode().isInsert()) return offset

        if (direction == MoveDirection.LEFT || direction == MoveDirection.RIGHT) {
            return offset
        }

        val foldingModel = editor.foldingModel
        // Get the most outer folded region at the current offset
        val foldRegion =
            foldingModel.getCollapsedRegionAtOffset(offset)
                ?: return offset

        // When moving DOWN into the first folded line, if the folded line is short and the cursor ends up at the end of
        // the line, it may be mistakenly judged as having entered the fold region.
        if (direction == MoveDirection.DOWN) {
            val startLine = editor.document.getLineNumber(offset)
            val foldStartLine = editor.document.getLineNumber(foldRegion.startOffset)
            if (startLine == foldStartLine) {
                return offset
            }
        }

        val adjustedLine =
            if (direction == MoveDirection.UP) {
                val startLine = editor.document.getLineNumber(foldRegion.startOffset)
                startLine
            } else {
                val endOffset = foldRegion.endOffset
                val endLine = editor.document.getLineNumber(endOffset)
                val maxLine = (editor.document.lineCount - 1).coerceAtLeast(0)
                (endLine + 1).coerceAtMost(maxLine)
            }
        val lineStartOffset = editor.document.getLineStartOffset(adjustedLine)
        val lineEndOffset = editor.document.getLineEndOffset(adjustedLine)
        val lineText = editor.document.getText(TextRange(lineStartOffset, lineEndOffset))
        val maxCol = (lineText.length - 1).coerceAtLeast(0)
        val curswantOffset = lineText.takeByte(curswant).length.coerceAtMost(maxCol)
        return lineStartOffset + curswantOffset
    }

    private suspend fun getScrollOptions(): Pair<Scrolloff, Sidescrolloff> {
        val options = optionManager.getLocal(bufferId)
        return options.scrolloff to options.sidescrolloff
    }

    private fun scrollLineIntoView(
        line: Int,
        scrolloff: Scrolloff,
    ) {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea
        val lineHeight = editor.lineHeight

        // Use VisualLine to respect the folded region
        val offset = editor.document.getLineStartOffset(line)
        val visualLine = editor.offsetToVisualLine(offset, false)

        val firstVisibleLine = editor.yToVisualLine(visibleArea.y)
        val lastVisibleLine = editor.yToVisualLine(visibleArea.y + visibleArea.height)

        val topLine = (visualLine - scrolloff.value).coerceAtLeast(0)
        val bottomLine = visualLine + scrolloff.value

        when {
            topLine < firstVisibleLine -> {
                val y = topLine * lineHeight
                scrollingModel.scrollVertically(y)
            }

            bottomLine >= lastVisibleLine -> {
                val y = bottomLine * lineHeight - visibleArea.height + lineHeight
                scrollingModel.scrollVertically(y)
            }
            // already in view
        }
    }

    private fun scrollColumnIntoView(
        column: Int,
        sidescrolloff: Sidescrolloff,
    ) {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea

        val firstVisibleColumn = visibleArea.x / fontSize.width
        val lastVisibleColumn = (visibleArea.x + visibleArea.width) / fontSize.width

        val targetLeft = column - sidescrolloff.value
        val targetRight = column + sidescrolloff.value

        when {
            targetLeft < firstVisibleColumn -> {
                val scrollToX = (targetLeft.coerceAtLeast(0) * fontSize.width)
                scrollingModel.scrollHorizontally(scrollToX)
            }

            targetRight > lastVisibleColumn -> {
                val colsToScroll = targetRight - lastVisibleColumn
                scrollingModel.scrollHorizontally(visibleArea.x + colsToScroll * fontSize.width)
            }
            // already in view
        }
    }

    fun syncIdeaToNeovim(position: NeovimPosition? = null) {
        val pos =
            position ?: runReadAction {
                val offset = editor.caretModel.offset
                NeovimPosition.fromOffset(offset, editor.document)
            }
        sendChan.trySend {
            logger.debug("Syncing cursor position to Neovim: $pos")
            client.setCursor(bufferId, pos)
        }
    }

    suspend fun changeCursorShape(
        oldMode: NeovimMode,
        newMode: NeovimMode,
    ) {
        val option = optionManager.getLocal(bufferId)
        withContext(Dispatchers.EDT) {
            if (oldMode.isCommand() && !newMode.isCommand()) {
                changeCaretVisible(true)
            } else if (!oldMode.isCommand() && newMode.isCommand()) {
                changeCaretVisible(false)
            }

            editor.settings.isBlockCursor = newMode.isBlock(option.selection)
        }
    }

    private fun changeCaretVisible(isVisible: Boolean) {
        editor.setCaretVisible(isVisible)
        editor.setCaretEnabled(isVisible)
        editor.contentComponent.repaint()
    }

    override fun dispose() {
        cursorListenerGuard.unregister()
    }
}
