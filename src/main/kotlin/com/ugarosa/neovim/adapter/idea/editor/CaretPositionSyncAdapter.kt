package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.bus.IdeaCaretMoved
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.bus.NvimCursorMoved
import com.ugarosa.neovim.common.FontSize
import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.config.nvim.option.Scrolloff
import com.ugarosa.neovim.config.nvim.option.Sidescrolloff
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.mode.getMode
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.domain.position.getLineText
import com.ugarosa.neovim.domain.position.takeByte
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CaretPositionSyncAdapter(
    private val editor: EditorEx,
    private val caretListener: IdeaCaretListener,
) {
    private val logger = myLogger()
    private val ideaToNvimBus = service<IdeaToNvimBus>()
    private val optionManager = service<NvimOptionManager>()

    suspend fun currentPosition(): NvimPosition =
        withContext(Dispatchers.EDT) {
            val offset = editor.caretModel.offset
            NvimPosition.fromOffset(offset, editor.document)
        }

    suspend fun apply(event: NvimCursorMoved) =
        withContext(Dispatchers.EDT) {
            val originalOffset = editor.caretModel.offset
            val rawOffset = event.pos.toOffset(editor.document)

            if (originalOffset == rawOffset) {
                logger.trace("No cursor move detected")
                return@withContext
            }

            logger.trace("Cursor move event: $event")

            val direction = determineDirection(originalOffset, rawOffset)

            val curswant = event.pos.curswant
            val adjustedOffset = adjustOffsetForFoldedRegion(rawOffset, curswant, direction)
            val (scrolloff, sidescrolloff) = getScrollOptions(event.bufferId)
            val fontSize = FontSize.fromEditorEx(editor)

            // LogicalPosition does not strictly match the number of characters, such as counting a hard tab as
            // multiple characters. Since it is difficult to calculate the appropriate position considering this, a
            // simpler and more accurate offset is used instead.
            runUndoTransparentWriteAction {
                caretListener.disable()
                editor.caretModel.moveToOffset(adjustedOffset)
                caretListener.enable()
                val pos = editor.offsetToLogicalPosition(adjustedOffset)
                scrollLineIntoView(pos.line, scrolloff)
                scrollColumnIntoView(pos.column, sidescrolloff, fontSize)
            }

            if (adjustedOffset != rawOffset) {
                // If the offset was adjusted, we need to update the cursor position in Neovim
                // Restore the original curswant
                val pos = currentPosition().copy(curswant = curswant)
                val offset = editor.caretModel.offset
                ideaToNvimBus.tryEmit(IdeaCaretMoved(event.bufferId, pos, offset))
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

        val foldingModel = editor.foldingModel
        // Get the most outer folded region at the current offset
        val foldRegion =
            foldingModel.getCollapsedRegionAtOffset(offset)
                ?: return offset

        when (direction) {
            MoveDirection.DOWN -> {
                // When moving DOWN into the first folded line, if the folded line is short and the cursor ends up at
                // the end of the line, it may be mistakenly judged as having entered the fold region.
                val startLine = editor.document.getLineNumber(offset)
                val foldStartLine = editor.document.getLineNumber(foldRegion.startOffset)
                if (startLine == foldStartLine) {
                    // should not expand the fold region
                    return offset.coerceAtMost(foldRegion.startOffset)
                }

                val endOffset = foldRegion.endOffset
                val endLine = editor.document.getLineNumber(endOffset)
                val maxLine = (editor.document.lineCount - 1).coerceAtLeast(0)
                val adjustedLine = (endLine + 1).coerceAtMost(maxLine)

                val lineText = editor.document.getLineText(adjustedLine)
                val maxCol = (lineText.length - 1).coerceAtLeast(0)
                val curswantCol = lineText.takeByte(curswant).length.coerceAtMost(maxCol)
                val lineStartOffset = editor.document.getLineStartOffset(adjustedLine)
                return lineStartOffset + curswantCol
            }

            MoveDirection.UP -> {
                val startLine = editor.document.getLineNumber(foldRegion.startOffset)
                val lineStartOffset = editor.document.getLineStartOffset(startLine)
                // should not expand the fold region
                val maxCol = foldRegion.startOffset - lineStartOffset
                val lineText = editor.document.getLineText(startLine)
                val curswantCol = lineText.takeByte(curswant).length.coerceAtMost(maxCol)
                return lineStartOffset + curswantCol
            }

            else -> {
                return offset
            }
        }
    }

    private suspend fun getScrollOptions(bufferId: BufferId): Pair<Scrolloff, Sidescrolloff> {
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
        fontSize: FontSize,
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
}
