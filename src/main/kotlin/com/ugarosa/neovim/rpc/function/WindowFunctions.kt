package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun enforceSingleWindow(client: NeovimRpcClient) {
    val luaCode = readLuaCode("/lua/enforceSingleWindow.lua") ?: return
    execLuaNotify(client, luaCode)
}
