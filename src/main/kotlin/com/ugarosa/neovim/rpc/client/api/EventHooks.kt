package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimObject

suspend fun NeovimClient.globalHooks() {
    execLuaNotify("hook", "global_hooks", listOf(chanId()))
}

suspend fun NeovimClient.localHooks(bufferId: NeovimObject.BufferId) {
    execLuaNotify("hook", "local_hooks", listOf(chanId(), bufferId))
}
