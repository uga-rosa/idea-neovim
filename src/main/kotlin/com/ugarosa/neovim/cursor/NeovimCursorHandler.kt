package com.ugarosa.neovim.cursor

import arrow.core.getOrElse
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.ugarosa.neovim.common.CARET_LISTENER_GUARD_KEY
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.SyncInhibitor
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.rpc.NeovimMode
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.getCursor
import com.ugarosa.neovim.rpc.setCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler(
    private val client: NeovimRpcClient,
    private val editor: Editor,
) {
    private val logger = thisLogger()
    private val syncInhibitor = SyncInhibitor()
    private val caretListenerGuard: ListenerGuard<NeovimCaretListener> =
        editor.getUserData(CARET_LISTENER_GUARD_KEY)
            ?: throw IllegalStateException("NeovimCaretListener not found in editor user data")

    // TODO: Make this configurable
    private val scrolloff = 3

    suspend fun syncCursorFromNeovimToIdea() {
        syncInhibitor.runIfAllowedSuspend {
            val nvimCursor =
                getCursor(client).getOrElse {
                    logger.warn("Failed to get Neovim cursor: $it")
                    return@runIfAllowedSuspend
                }
            val pos = nvimCursor.toLogicalPosition()
            caretListenerGuard.runWithoutListenerSuspend {
                withContext(Dispatchers.EDT) {
                    editor.caretModel.moveToLogicalPosition(pos)
                    scrollLineIntoView(pos.line)
                }
            }
        }
    }

    private fun scrollLineIntoView(line: Int) {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea
        val lineHeight = editor.lineHeight

        val firstVisibleLine = editor.yToVisualLine(visibleArea.y)
        val lastVisibleLine = editor.yToVisualLine(visibleArea.y + visibleArea.height)

        val targetTop = line - scrolloff
        val targetBottom = line + scrolloff

        when {
            targetTop < firstVisibleLine -> {
                val scrollToY = targetTop.coerceAtLeast(0) * lineHeight
                scrollingModel.scrollVertically(scrollToY)
            }

            targetBottom > lastVisibleLine -> {
                val linesToScroll = targetBottom - lastVisibleLine
                scrollingModel.scrollVertically(visibleArea.y + linesToScroll * lineHeight)
            }
            // Do nothing if the line is already in view
        }
    }

    suspend fun syncCursorFromIdeaToNeovim() {
        syncInhibitor.runIfAllowedSuspend {
            val logicalPosition = editor.caretModel.logicalPosition
            val nvimCursor = logicalPosition.toNeovimPosition()
            setCursor(client, nvimCursor)
        }
    }

    fun changeCursorShape(mode: NeovimMode) {
        editor.settings.isBlockCursor = mode in
            setOf(
                NeovimMode.NORMAL,
                NeovimMode.VISUAL,
                NeovimMode.VISUAL_LINE,
                NeovimMode.VISUAL_BLOCK,
                NeovimMode.SELECT,
            )
    }

    private fun Pair<Int, Int>.toLogicalPosition(): LogicalPosition {
        // Neovim uses (1, 0) byte-based indexing
        // IntelliJ uses 0-based line indexing
        val document = editor.document
        val lineIndex = this.first - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val lineText = document.text.substring(lineStartOffset, lineEndOffset)
        val correctedCol = utf8ByteOffsetToCharOffset(lineText, this.second)

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
