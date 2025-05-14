package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.BufferId

suspend fun NeovimClient.changedTick(bufferId: BufferId): Long {
    return request("nvim_buf_get_var", listOf(bufferId, "changedtick")).asLong()
}
