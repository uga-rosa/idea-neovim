package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.rpc.client.NvimClient

const val CHANGED_TICK = "changedtick"

suspend fun NvimClient.bufVar(
    bufferId: BufferId,
    name: String,
): Long {
    return request("nvim_buf_get_var", listOf(bufferId, name)).asLong()
}
