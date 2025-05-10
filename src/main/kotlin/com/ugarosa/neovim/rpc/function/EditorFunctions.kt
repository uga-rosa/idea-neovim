package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.domain.NeovimPosition
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun input(
    client: NeovimRpcClient,
    key: String,
): Unit = client.notify("nvim_input", listOf(key))

suspend fun setCursor(
    client: NeovimRpcClient,
    bufferId: BufferId,
    pos: NeovimPosition,
) {
    val luaCode = readLuaCode("/lua/setCursorEventIgnore.lua") ?: return
    execLuaNotify(client, luaCode, listOf(bufferId, pos.lnum, pos.col + 1, pos.curswant))
}
