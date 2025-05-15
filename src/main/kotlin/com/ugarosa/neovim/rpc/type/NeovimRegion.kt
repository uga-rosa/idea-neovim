package com.ugarosa.neovim.rpc.type

import com.intellij.openapi.editor.Document

// (0, 0) index, column is byte offset, end-exclusive
data class NeovimRegion(
    val row: Int,
    val startColumn: Int,
    val endColumn: Int,
) {
    fun startOffset(document: Document): Int {
        return NeovimPosition(row, startColumn).toOffset(document)
    }

    fun endOffset(document: Document): Int {
        val offset = NeovimPosition(row, endColumn).toOffset(document)
        val line = document.getLineNumber(offset)
        val lineEndOffset = document.getLineEndOffset(line)
        return offset.coerceAtMost(lineEndOffset)
    }
}
