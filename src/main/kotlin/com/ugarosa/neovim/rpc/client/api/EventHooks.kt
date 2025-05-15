package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient

suspend fun NeovimClient.globalHooks() {
    val chanId = deferredChanId.await()
    execLuaNotify("hook", "global_hooks", listOf(chanId))
}

suspend fun NeovimClient.localHooks(bufferId: BufferId) {
    val chanId = deferredChanId.await()
    execLuaNotify("hook", "local_hooks", listOf(chanId, bufferId))
}
