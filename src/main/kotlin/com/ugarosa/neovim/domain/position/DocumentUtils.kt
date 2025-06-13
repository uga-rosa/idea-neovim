package com.ugarosa.neovim.domain.position

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

data class DocumentLine(
    val line: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

fun Document.getLineText(line: Int): DocumentLine {
    require(line >= 0) { "Line number must be non-negative" }
    require(line < lineCount || (line == lineCount && lineCount == 0)) { "Line number exceeds the number of lines in the document" }
    val lineStartOffset = getLineStartOffset(line)
    val lineEndOffset = getLineEndOffset(line)
    val text = getText(TextRange(lineStartOffset, lineEndOffset))
    return DocumentLine(line, text, lineStartOffset, lineEndOffset)
}
