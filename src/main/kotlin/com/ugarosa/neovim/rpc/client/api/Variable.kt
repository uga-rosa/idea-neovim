package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimObject

suspend fun NeovimClient.changedTick(bufferId: NeovimObject.BufferId): Long {
    val result = connectionManager.request("nvim_buf_get_var", listOf(bufferId, "changedtick"))
    return result.asInt64().long
}
