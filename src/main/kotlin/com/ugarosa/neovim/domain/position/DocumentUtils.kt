package com.ugarosa.neovim.domain.position

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

fun Document.getLineText(line: Int): String {
    require(line >= 0) { "Line number must be non-negative" }
    require(line < lineCount) { "Line number exceeds the number of lines in the document" }
    val lineStartOffset = getLineStartOffset(line)
    val lineEndOffset = getLineEndOffset(line)
    val text = getText(TextRange(lineStartOffset, lineEndOffset))
    return text
}
