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
    val luaCode = readLuaCode("/lua/activateBufferEventIgnore.lua") ?: return
    execLuaNotify(client, luaCode, listOf(bufferId))
}

suspend fun bufferSetLines(
    client: NeovimRpcClient,
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
): Unit = client.notify("nvim_buf_set_lines", listOf(bufferId, start, end, false, lines))

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
): Unit =
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

suspend fun bufferAttach(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Unit = client.notify("nvim_buf_attach", listOf(bufferId, false, emptyMap<String, Any>()))

suspend fun setFiletype(
    client: NeovimRpcClient,
    bufferId: BufferId,
    path: String,
) {
    val luaCode = readLuaCode("/lua/setFiletype.lua") ?: return
    execLuaNotify(client, luaCode, listOf(bufferId, path))
}
