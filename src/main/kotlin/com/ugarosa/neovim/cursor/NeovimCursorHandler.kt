package com.ugarosa.neovim.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.SyncInhibitor
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.function.NeovimMode
import com.ugarosa.neovim.rpc.function.setCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler(
    private val client: NeovimRpcClient,
    private val editor: Editor,
    private val bufferId: BufferId,
    private val disposable: Disposable,
) {
    private val logger = thisLogger()
    private val syncInhibitor = SyncInhibitor()
    private val caretListenerGuard =
        ListenerGuard(
            NeovimCaretListener(editor),
            { editor.caretModel.addCaretListener(it, disposable) },
            { editor.caretModel.removeCaretListener(it) },
        ).apply {
            register()
        }

    private val optionManager = ApplicationManager.getApplication().service<NeovimOptionManager>()

    suspend fun syncCursorFromNeovimToIdea(event: CursorMoveEvent) {
        syncInhibitor.runIfAllowedSuspend {
            val pos = event.toLogicalPosition()
            caretListenerGuard.runWithoutListenerSuspend {
                withContext(Dispatchers.EDT) {
                    editor.caretModel.moveToLogicalPosition(pos)
                    val (scrolloff, sidescrolloff) = getScrollOptions()
                    scrollLineIntoView(pos.line, scrolloff)
                    scrollColumnIntoView(pos.column, sidescrolloff)
                }
            }
        }
    }

    private suspend fun getScrollOptions(): Pair<Scrolloff, Sidescrolloff> {
        val options = optionManager.getLocal(bufferId)
        logger.trace("Got current local options: $options")
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

    suspend fun syncCursorFromIdeaToNeovim() {
        syncInhibitor.runIfAllowedSuspend {
            val logicalPosition = editor.caretModel.logicalPosition
            val nvimCursor = logicalPosition.toNeovimPosition()
            setCursor(client, nvimCursor)
        }
    }

    suspend fun changeCursorShape(mode: NeovimMode) {
        withContext(Dispatchers.EDT) {
            editor.settings.isBlockCursor = mode in
                setOf(
                    NeovimMode.NORMAL,
                    NeovimMode.VISUAL,
                    NeovimMode.VISUAL_LINE,
                    NeovimMode.VISUAL_BLOCK,
                    NeovimMode.SELECT,
                )
        }
    }

    private fun CursorMoveEvent.toLogicalPosition(): LogicalPosition {
        // Neovim uses (1, 0) byte-based indexing
        // IntelliJ uses 0-based line indexing
        val document = editor.document
        val lineIndex = this.line - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val lineText = document.text.substring(lineStartOffset, lineEndOffset)
        val correctedCol = utf8ByteOffsetToCharOffset(lineText, this.column)

        return LogicalPosition(lineIndex, correctedCol)
    }

    private fun LogicalPosition.toNeovimPosition(): Pair<Int, Int> {
        // Neovim uses (1, 0) byte-based indexing
        // IntelliJ uses 0-based line indexing
        val document = editor.document
        val line = this.line

        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.text.substring(lineStartOffset, lineEndOffset)
        val byteCol = charOffsetToUtf8ByteOffset(lineText, this.column)

        return line + 1 to byteCol
    }
}
