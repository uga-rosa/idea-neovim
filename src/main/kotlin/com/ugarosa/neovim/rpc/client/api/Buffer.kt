package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.BufferId
import com.ugarosa.neovim.rpc.type.NeovimPosition

suspend fun NeovimClient.createBuffer(): BufferId {
    val result = connectionManager.request("nvim_create_buf", listOf(true, false))
    return result.asBufferId()
}

suspend fun NeovimClient.activateBuffer(bufferId: BufferId) {
    execLuaNotify("buffer", "activate", listOf(bufferId))
}

suspend fun NeovimClient.bufferSetLines(
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
) {
    connectionManager.notify(
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
    connectionManager.notify(
        "nvim_buf_set_text",
        listOf(
            bufferId,
            start.lnum,
            start.col,
            end.lnum,
            end.col,
            replacement,
        ),
    )
}

suspend fun NeovimClient.bufferAttach(bufferId: BufferId) {
    connectionManager.notify("nvim_buf_attach", listOf(bufferId, false, mapOf<String, Any>()))
}
