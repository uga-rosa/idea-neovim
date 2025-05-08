package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.common.asStringMap
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun getGlobalOptions(client: NeovimRpcClient): Map<String, Any>? {
    val luaCode = readLuaCode("/lua/getGlobalOptions.lua") ?: return null
    return execLua(client, luaCode)?.decode { it.asStringMap() }
}

suspend fun getLocalOptions(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Map<String, Any>? {
    val luaCode = readLuaCode("/lua/getLocalOptions.lua") ?: return null
    return execLua(client, luaCode, listOf(bufferId))
        ?.decode { it.asStringMap() }
}

suspend fun hookGlobalOptionSet(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookGlobalOptionSet.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}

suspend fun hookLocalOptionSet(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookLocalOptionSet.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId, bufferId))
}
