package com.ugarosa.neovim.domain.position

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

// (0, 0) index, column is byte offset
data class NvimPosition(
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
        ): NvimPosition {
            val line = document.getLineNumber(offset)
            val documentLine = document.getLineText(line)
            val byteCol = charOffsetToUtf8ByteOffset(documentLine.text, offset - documentLine.startOffset)
            return NvimPosition(line, byteCol)
        }
    }
}
