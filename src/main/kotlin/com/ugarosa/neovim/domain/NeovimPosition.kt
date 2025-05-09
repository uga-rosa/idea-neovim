package com.ugarosa.neovim.domain

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset

// (1, 0) index, column is byte offset
data class NeovimPosition(
    // 1-based
    val row: Int,
    // 0-based byte offset
    val col: Int,
) {
    val line: Int = row - 1

    fun toLogicalPosition(document: Document): LogicalPosition {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val colChar = utf8ByteOffsetToCharOffset(lineText, col)
        return LogicalPosition(line, colChar)
    }

    companion object {
        fun fromLogicalPosition(
            logicalPosition: LogicalPosition,
            document: Document,
        ): NeovimPosition {
            val lineStartOffset = document.getLineStartOffset(logicalPosition.line)
            val lineEndOffset = document.getLineEndOffset(logicalPosition.line)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            val byteCol = charOffsetToUtf8ByteOffset(lineText, logicalPosition.column)

            return NeovimPosition(logicalPosition.line + 1, byteCol)
        }

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
