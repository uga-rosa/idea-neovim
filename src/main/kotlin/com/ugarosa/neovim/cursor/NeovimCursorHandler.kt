package com.ugarosa.neovim.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.takeByte
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.domain.NeovimPosition
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.function.setCursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler private constructor(
    scope: CoroutineScope,
    private val editor: Editor,
    private val disposable: Disposable,
    private val bufferId: BufferId,
    private val charWidth: Int,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val modeManager = getModeManager()
    private val optionManager = getOptionManager()
    private val caretListenerGuard =
        ListenerGuard(
            NeovimCaretListener(scope, this),
            { editor.caretModel.addCaretListener(it, disposable) },
            { editor.caretModel.removeCaretListener(it) },
        )

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            disposable: Disposable,
            bufferId: BufferId,
        ): NeovimCursorHandler {
            val charWidth =
                withContext(Dispatchers.EDT) {
                    val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
                    editor.contentComponent.getFontMetrics(font).charWidth('W')
                }
            val handler = NeovimCursorHandler(scope, editor, disposable, bufferId, charWidth)
            withContext(Dispatchers.EDT) {
                editor.settings.isBlockCursor = true
            }
            handler.enableCursorListener()
            return handler
        }
    }

    fun enableCursorListener() {
        logger.trace("Enabling cursor listener for buffer: $bufferId")
        caretListenerGuard.register()
    }

    fun disableCursorListener() {
        logger.trace("Disabling cursor listener for buffer: $bufferId")
        caretListenerGuard.unregister()
    }

    suspend fun syncNeovimToIdea(event: CursorMoveEvent) {
        val originalOffset = runReadAction { editor.caretModel.offset }
        val rawOffset = runReadAction { event.position.toOffset(editor.document) }
        val direction = runReadAction { determineDirection(originalOffset, rawOffset) }

        val curswant = event.position.curswant
        val adjustedOffset = runReadAction { adjustOffsetForFoldedRegion(rawOffset, curswant, direction) }
        val (scrolloff, sidescrolloff) = getScrollOptions()

        caretListenerGuard.runWithoutListenerSuspend {
            withContext(Dispatchers.EDT) {
                if (direction == MoveDirection.LEFT || direction == MoveDirection.RIGHT) {
                    editor.foldingModel.getCollapsedRegionAtOffset(adjustedOffset)?.let { region ->
                        editor.foldingModel.runBatchFoldingOperation {
                            region.isExpanded = true
                        }
                    }
                }

                // LogicalPosition does not strictly match the number of characters, such as counting a hard tab as
                // multiple characters. Since it is difficult to calculate the appropriate position considering this, a
                // simpler and more accurate offset is used instead.
                editor.caretModel.moveToOffset(adjustedOffset)
                val pos = editor.offsetToLogicalPosition(adjustedOffset)
                scrollLineIntoView(pos.line, scrolloff)
                scrollColumnIntoView(pos.column, sidescrolloff)
            }
        }

        if (adjustedOffset != rawOffset) {
            // If the offset was adjusted, we need to update the cursor position in Neovim
            // Restore the original curswant
            val position =
                runReadAction {
                    val offset = editor.caretModel.offset
                    NeovimPosition.fromOffset(offset, editor.document)
                        .copy(curswant = curswant)
                }
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

    private fun adjustOffsetForFoldedRegion(
        offset: Int,
        curswant: Int,
        direction: MoveDirection,
    ): Int {
        if (modeManager.getMode().isInsert()) return offset

        if (direction == MoveDirection.LEFT || direction == MoveDirection.RIGHT) {
            return offset
        }

        val foldingModel = editor.foldingModel
        // Get the most outer folded region at the current offset
        val foldRegion =
            foldingModel.getCollapsedRegionAtOffset(offset)
                ?: return offset

        val adjustedLine =
            if (direction == MoveDirection.UP) {
                val startLine = editor.document.getLineNumber(foldRegion.startOffset)
                startLine
            } else {
                val endOffset = foldRegion.endOffset + 1
                val endLine = editor.document.getLineNumber(endOffset)
                (endLine + 1).coerceAtMost(editor.document.lineCount - 1)
            }
        val lineStartOffset = editor.document.getLineStartOffset(adjustedLine)
        val lineEndOffset = editor.document.getLineEndOffset(adjustedLine)
        val lineText = editor.document.getText(TextRange(lineStartOffset, lineEndOffset))
        val curswantOffset = lineText.takeByte(curswant - 1).length.coerceAtMost(lineText.length - 1)
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

        val firstVisibleLine = editor.yToVisualLine(visibleArea.y)
        val lastVisibleLine = editor.yToVisualLine(visibleArea.y + visibleArea.height)

        val targetTop = line - scrolloff.value
        val targetBottom = line + scrolloff.value

        when {
            targetTop < firstVisibleLine -> {
                val scrollToY = targetTop.coerceAtLeast(0) * lineHeight
                scrollingModel.scrollVertically(scrollToY)
            }

            targetBottom > lastVisibleLine -> {
                val linesToScroll = targetBottom - lastVisibleLine
                scrollingModel.scrollVertically(visibleArea.y + linesToScroll * lineHeight)
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

        val firstVisibleColumn = visibleArea.x / charWidth
        val lastVisibleColumn = (visibleArea.x + visibleArea.width) / charWidth

        val targetLeft = column - sidescrolloff.value
        val targetRight = column + sidescrolloff.value

        when {
            targetLeft < firstVisibleColumn -> {
                val scrollToX = (targetLeft.coerceAtLeast(0) * charWidth)
                scrollingModel.scrollHorizontally(scrollToX)
            }

            targetRight > lastVisibleColumn -> {
                val colsToScroll = targetRight - lastVisibleColumn
                scrollingModel.scrollHorizontally(visibleArea.x + colsToScroll * charWidth)
            }
            // already in view
        }
    }

    suspend fun syncIdeaToNeovim(position: NeovimPosition? = null) {
        val pos =
            position ?: runReadAction {
                val offset = editor.caretModel.offset
                NeovimPosition.fromOffset(offset, editor.document)
            }
        setCursor(client, pos)
    }

    suspend fun changeCursorShape(mode: NeovimMode) {
        val option = optionManager.getLocal(bufferId)
        withContext(Dispatchers.EDT) {
            editor.settings.isBlockCursor = mode.isBlock(option.selection)
        }
    }
}
