package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun createCommand(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/createCommand.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}
