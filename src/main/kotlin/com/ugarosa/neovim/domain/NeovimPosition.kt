package com.ugarosa.neovim.domain

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset

// (1, 0) index, column is byte offset
data class NeovimPosition(
    // 1-based
    val lnum: Int,
    // 0-based byte offset
    val col: Int,
    // As is from Neovim. 1-based.
    // `:h getcurpos()`
    val curswant: Int = col + 1,
) {
    fun toOffset(document: Document): Int {
        val lineStartOffset = document.getLineStartOffset(lnum - 1)
        val lineEndOffset = document.getLineEndOffset(lnum - 1)
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

            return NeovimPosition(line + 1, byteCol)
        }
    }
}
