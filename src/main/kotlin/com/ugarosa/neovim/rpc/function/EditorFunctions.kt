package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun input(
    client: NeovimRpcClient,
    key: String,
): Unit = client.notify("nvim_input", listOf(key))

suspend fun hookCursorMove(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookCursorMove.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}

suspend fun setCursor(
    client: NeovimRpcClient,
    row: Int,
    col: Int,
) {
    val luaCode = readLuaCode("/lua/setCursorWithoutEvent.lua") ?: return
    execLuaNotify(client, luaCode, listOf(row, col))
}
