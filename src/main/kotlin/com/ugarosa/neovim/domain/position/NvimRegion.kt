package com.ugarosa.neovim.domain.position

import com.intellij.openapi.editor.Document

// (0, 0) index, column is byte offset, end-exclusive
data class NvimRegion(
    val row: Int,
    val startColumn: Int,
    val endColumn: Int,
) {
    fun startOffset(document: Document): Int {
        return NvimPosition(row, startColumn).toOffset(document)
    }

    fun endOffset(document: Document): Int {
        val offset = NvimPosition(row, endColumn).toOffset(document)
        val line = document.getLineNumber(offset)
        val lineEndOffset = document.getLineEndOffset(line)
        return offset.coerceAtMost(lineEndOffset)
    }
}
