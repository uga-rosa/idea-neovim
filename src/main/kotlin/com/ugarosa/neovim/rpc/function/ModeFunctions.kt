package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun hookModeChange(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookModeChange.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}
