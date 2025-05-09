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
