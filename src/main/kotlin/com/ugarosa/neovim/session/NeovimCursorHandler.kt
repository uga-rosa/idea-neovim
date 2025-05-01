package com.ugarosa.neovim.session

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.infra.NeovimRpcClient
import com.ugarosa.neovim.rpc.NeovimFunctions
import com.ugarosa.neovim.rpc.NeovimMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlin.jvm.Throws

class NeovimCursorHandler(
    private val rpcClient: NeovimRpcClient,
    private val editor: Editor,
) {
    suspend fun syncCursorFromNeovimToIdea() {
        val pos =
            try {
                getNeovimCursorPosition()
            } catch (e: TimeoutCancellationException) {
                // Ignore
                return
            }
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

    @Throws(TimeoutCancellationException::class)
    private suspend fun getNeovimCursorPosition(): LogicalPosition {
        // Neovim uses (1, 0) byte-based indexing
        val (nvimRow, nvimByteCol) = NeovimFunctions.getCursor(rpcClient)
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
