package com.ugarosa.neovim.rpc.type

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset

// (0, 0) index, column is byte offset
data class NeovimPosition(
    val line: Int,
    val col: Int,
    // Adjusted to 0-based.
    // `:h getcurpos()`
    val curswant: Int = col,
) {
    fun toOffset(document: Document): Int {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val colChar = utf8ByteOffsetToCharOffset(lineText, col)
        return (lineStartOffset + colChar).coerceAtMost(lineEndOffset)
    }

    companion object {
        fun fromOffset(
            offset: Int,
            document: Document,
        ): NeovimPosition {
            val line = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(line)
            val lineEndOffset = document.getLineEndOffset(line)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            val byteCol = charOffsetToUtf8ByteOffset(lineText, offset - lineStartOffset)

            return NeovimPosition(line, byteCol)
        }
    }
}
