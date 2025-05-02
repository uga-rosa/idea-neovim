package com.ugarosa.neovim.session

import arrow.core.getOrElse
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.rpc.NeovimClient
import com.ugarosa.neovim.rpc.NeovimMode
import com.ugarosa.neovim.rpc.getCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler(
    private val client: NeovimClient,
    private val editor: Editor,
) {
    private val logger = thisLogger()

    suspend fun syncCursorFromNeovimToIdea() {
        val pos =
            getNeovimCursorPosition()
                ?: return
        withContext(Dispatchers.EDT) {
            editor.caretModel.moveToLogicalPosition(pos)
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

    private suspend fun getNeovimCursorPosition(): LogicalPosition? {
        // Neovim uses (1, 0) byte-based indexing
        val (nvimRow, nvimByteCol) =
            getCursor(client).getOrElse {
                logger.warn("Failed to get Neovim cursor: $it")
                return null
            }
        val document = editor.document
        // IntelliJ uses 0-based line indexing
        val lineIndex = nvimRow - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val lineText = document.text.substring(lineStartOffset, lineEndOffset)
        val correctedCol = utf8ByteOffsetToCharOffset(lineText, nvimByteCol)

        return LogicalPosition(lineIndex, correctedCol)
    }
}
