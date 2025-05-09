package com.ugarosa.neovim.common

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.util.TextRange

// (1, 0) index, column is byte offset
data class NeovimPosition(
    val row: Int,
    val col: Int,
) {
    val line: Int = row - 1
}

fun NeovimPosition.toLogicalPosition(document: Document): LogicalPosition {
    val lineStartOffset = document.getLineStartOffset(line)
    val lineEndOffset = document.getLineEndOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
    val colChar = utf8ByteOffsetToCharOffset(lineText, col)
    return LogicalPosition(line, colChar)
}

fun LogicalPosition.toNeovimPosition(document: Document): NeovimPosition {
    val lineStartOffset = document.getLineStartOffset(line)
    val lineEndOffset = document.getLineEndOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
    val byteCol = charOffsetToUtf8ByteOffset(lineText, this.column)

    return NeovimPosition(line + 1, byteCol)
}

// offset to NeovimPosition
fun Int.toNeovimPosition(document: Document): NeovimPosition {
    val offset = this
    val line = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(line)
    val lineEndOffset = document.getLineEndOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
    val byteCol = charOffsetToUtf8ByteOffset(lineText, offset - lineStartOffset)

    return NeovimPosition(line + 1, byteCol)
}
