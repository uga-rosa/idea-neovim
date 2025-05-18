package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimPosition

suspend fun NeovimClient.createBuffer(): BufferId {
    val result = request("nvim_create_buf", listOf(true, false))
    return result.asBufferId()
}

suspend fun NeovimClient.bufferSetLines(
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
) {
    // Indexing is zero-based, end-exclusive. Negative indices are interpreted as length+1+index: -1 refers to the index
    // past the end. So to change or delete the last line use start=-2 and end=-1.
    request(
        "nvim_buf_set_lines",
        listOf(bufferId, start, end, false, lines),
    )
}

suspend fun NeovimClient.bufferSetText(
    bufferId: BufferId,
    start: NeovimPosition,
    end: NeovimPosition,
    replacement: List<String>,
) {
    // Indexing is zero-based. Row indices are end-inclusive, and column indices are end-exclusive.
    request(
        "nvim_buf_set_text",
        listOf(
            bufferId,
            start.line,
            start.col,
            end.line,
            end.col,
            replacement,
        ),
    )
}

suspend fun NeovimClient.bufferAttach(bufferId: BufferId) {
    notify("nvim_buf_attach", listOf(bufferId, false, mapOf<String, Any>()))
}
