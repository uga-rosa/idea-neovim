package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.BufferId

suspend fun NeovimClient.globalHooks() {
    val chanId = deferredChanId.await()
    execLuaNotify("hook", "global_hooks", listOf(chanId))
}

suspend fun NeovimClient.localHooks(bufferId: BufferId) {
    val chanId = deferredChanId.await()
    execLuaNotify("hook", "local_hooks", listOf(chanId, bufferId))
}
