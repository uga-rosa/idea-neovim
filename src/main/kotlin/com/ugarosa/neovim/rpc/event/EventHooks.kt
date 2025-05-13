package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify

suspend fun globalHooks(client: NeovimRpcClient) {
    val chanId = ChanIdManager.get()
    execLuaNotify(client, "hook", "global_hooks", listOf(chanId))
}

suspend fun localHooks(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    val chanId = ChanIdManager.get()
    execLuaNotify(client, "hook", "local_hooks", listOf(chanId, bufferId))
}
