package com.ugarosa.neovim.domain

import com.intellij.openapi.editor.Document

data class NeovimRegion(
    // 1-based
    val row: Int,
    // 0-based, byte offset
    val startColumn: Int,
    // 0-based, byte offset
    val endColumn: Int,
) {
    fun startOffset(document: Document): Int {
        return NeovimPosition(row, startColumn).toOffset(document)
    }

    fun endOffset(document: Document): Int {
        val offset = NeovimPosition(row, endColumn).toOffset(document)
        val line = document.getLineNumber(offset)
        val lineEndOffset = document.getLineEndOffset(line)
        return (offset + 1).coerceAtMost(lineEndOffset)
    }
}
