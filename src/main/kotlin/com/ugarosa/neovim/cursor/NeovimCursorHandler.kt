package com.ugarosa.neovim.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.event.NeovimMode
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import com.ugarosa.neovim.rpc.function.setCursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler private constructor(
    scope: CoroutineScope,
    private val editor: Editor,
    private val disposable: Disposable,
    private val bufferId: BufferId,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val caretListenerGuard =
        ListenerGuard(
            NeovimCaretListener(scope, this),
            { editor.caretModel.addCaretListener(it, disposable) },
            { editor.caretModel.removeCaretListener(it) },
        ).apply {
            logger.trace("Registering caret listener for buffer: $bufferId")
            register()
        }

    private val optionManager = getOptionManager()

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            disposable: Disposable,
            bufferId: BufferId,
        ): NeovimCursorHandler {
            withContext(Dispatchers.EDT) {
                editor.settings.isBlockCursor = true
            }
            return NeovimCursorHandler(scope, editor, disposable, bufferId)
        }
    }

    fun enableCursorListener() {
        caretListenerGuard.register()
    }

    fun disableCursorListener() {
        caretListenerGuard.unregister()
    }

    suspend fun syncNeovimToIdea(event: CursorMoveEvent) {
        val pos = toLogicalPosition(event.line, event.column)
        caretListenerGuard.runWithoutListenerSuspend {
            withContext(Dispatchers.EDT) {
                editor.caretModel.moveToLogicalPosition(pos)
                val (scrolloff, sidescrolloff) = getScrollOptions()
                scrollLineIntoView(pos.line, scrolloff)
                scrollColumnIntoView(pos.column, sidescrolloff)
            }
        }
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

    private val charWidth: Int by lazy {
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        editor.contentComponent.getFontMetrics(font)
            .charWidth('W')
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

    suspend fun syncIdeaToNeovim() {
        val logicalPosition = editor.caretModel.logicalPosition
        val (row, col) = logicalPosition.toNeovimPosition()
        setCursor(client, row, col)
    }

    suspend fun changeCursorShape(mode: NeovimMode) {
        withContext(Dispatchers.EDT) {
            editor.settings.isBlockCursor =
                when (mode.kind) {
                    NeovimModeKind.NORMAL,
                    NeovimModeKind.VISUAL,
                    NeovimModeKind.VISUAL_LINE,
                    NeovimModeKind.VISUAL_BLOCK,
                    NeovimModeKind.SELECT,
                    NeovimModeKind.SELECT_LINE,
                    NeovimModeKind.SELECT_BLOCK,
                    -> true

                    else -> false
                }
        }
    }

    private fun toLogicalPosition(
        nvimRow: Int,
        nvimCol: Int,
    ): LogicalPosition {
        // Neovim uses (1, 0) byte-based indexing
        // IntelliJ uses 0-based line indexing
        val document = editor.document
        val lineIndex = nvimRow - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val correctedCol = utf8ByteOffsetToCharOffset(lineText, nvimCol)

        return LogicalPosition(lineIndex, correctedCol)
    }

    private fun LogicalPosition.toNeovimPosition(): Pair<Int, Int> {
        // Neovim uses (1, 0) byte-based indexing
        // IntelliJ uses 0-based line indexing
        val document = editor.document
        val line = this.line

        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val byteCol = charOffsetToUtf8ByteOffset(lineText, this.column)

        return line + 1 to byteCol
    }
}
