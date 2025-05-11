package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.asBufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun createBuffer(client: NeovimRpcClient): BufferId? =
    client.request("nvim_create_buf", listOf(true, false))
        ?.decode { it.asBufferId() }

suspend fun activateBuffer(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    execLuaNotify(client, "buffer", "activate", listOf(bufferId))
}

suspend fun bufferSetLines(
    client: NeovimRpcClient,
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
) {
    client.notify("nvim_buf_set_lines", listOf(bufferId, start, end, false, lines))
}

data class BufferSetTextParams(
    val bufferId: BufferId,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val replacement: List<String>,
)

suspend fun bufferSetText(
    client: NeovimRpcClient,
    params: BufferSetTextParams,
) {
    client.notify(
        "nvim_buf_set_text",
        listOf(
            params.bufferId,
            params.startRow,
            params.startCol,
            params.endRow,
            params.endCol,
            params.replacement,
        ),
    )
}

suspend fun bufferAttach(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    client.notify("nvim_buf_attach", listOf(bufferId, false, emptyMap<String, Any>()))
}

suspend fun setFiletype(
    client: NeovimRpcClient,
    bufferId: BufferId,
    path: String,
) {
    execLuaNotify(client, "option", "set_filetype", listOf(bufferId, path))
}

suspend fun noModifiable(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    execLuaNotify(client, "option", "set_no_writable", listOf(bufferId))
}

suspend fun modifiable(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    execLuaNotify(client, "option", "set_writable", listOf(bufferId))
}
