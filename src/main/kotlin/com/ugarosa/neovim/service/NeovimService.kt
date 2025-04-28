package com.ugarosa.neovim.service

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.infra.NeovimRpcClient

class NeovimService(
    private val client: NeovimRpcClient,
) {
    fun initializeNeovimBuffer(editor: Editor) {
        val document = editor.document
        val text = document.text
        val lines = text.split("\n")

        client.sendRequest(
            "nvim_buf_set_lines",
            listOf(0, 0, -1, false, lines),
        )
        client.receiveResponse()
    }

    fun sendInput(key: String) {
        client.sendRequest("nvim_input", listOf(key))
        client.receiveResponse()
    }

    fun getCursor(editor: Editor): LogicalPosition {
        val document = editor.document

        client.sendRequest("nvim_win_get_cursor", listOf(0))
        val response = client.receiveResponse()

        val cursorArray = response.result.asArrayValue().list()
        // Neovim uses (1, 0) byte-based indexing
        val nvimRow = cursorArray[0].asIntegerValue().asInt()
        val nvimByteCol = cursorArray[1].asIntegerValue().asInt()

        val lineIndex = nvimRow - 1 // IntelliJ uses 0-based line indexing

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
