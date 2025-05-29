package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.buffer.RepeatableChange
import com.ugarosa.neovim.domain.id.BufferId
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
    request(
        "nvim_buf_set_lines",
        listOf(bufferId, start, end, false, lines),
    )
}

suspend fun NvimClient.bufferSetText(
    bufferId: BufferId,
    start: Int,
    end: Int,
    replacement: List<String>,
) {
    execLuaNotify("buffer", "set_text", listOf(bufferId, start, end, replacement))
}

suspend fun NvimClient.sendRepeatableChange(change: RepeatableChange) {
    execLuaNotify(
        "buffer",
        "send_repeatable_change",
        listOf(
            change.leftDel,
            change.rightDel,
            change.body.toString(),
        ),
    )
}

suspend fun NvimClient.bufferAttach(bufferId: BufferId) {
    request("nvim_buf_attach", listOf(bufferId, false, mapOf<String, Any>()))
}
