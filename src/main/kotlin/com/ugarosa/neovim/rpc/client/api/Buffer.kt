package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.createBuffer(): BufferId {
    val result = request("nvim_create_buf", listOf(true, false))
    return result.asBufferId()
}

suspend fun NvimClient.bufferSetLines(
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
) {
    // Indexing is zero-based, end-exclusive. Negative indices are interpreted as length+1+index: -1 refers to the index
    // past the end. So to change or delete the last line use start=-2 and end=-1.
    request("nvim_buf_set_lines", listOf(bufferId, start, end, false, lines))
}

suspend fun NvimClient.bufferSetText(
    bufferId: BufferId,
    start: NvimPosition,
    end: NvimPosition,
    replacement: List<String>,
) {
    notify("nvim_buf_set_text", listOf(bufferId, start.line, start.col, end.line, end.col, replacement))
}

suspend fun NvimClient.bufferAttach(bufferId: BufferId) {
    request("nvim_buf_attach", listOf(bufferId, false, mapOf<String, Any>()))
}
