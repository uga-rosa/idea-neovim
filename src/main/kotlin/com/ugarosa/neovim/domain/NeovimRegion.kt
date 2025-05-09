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
        val pos = NeovimPosition(row, startColumn).toLogicalPosition(document)
        return document.getLineStartOffset(pos.line) + pos.column
    }

    fun endOffset(document: Document): Int {
        val pos = NeovimPosition(row, endColumn).toLogicalPosition(document)
        val lineEndOffset = document.getLineEndOffset(pos.line)
        return (document.getLineStartOffset(pos.line) + pos.column + 1)
            .coerceAtMost(lineEndOffset)
    }
}
