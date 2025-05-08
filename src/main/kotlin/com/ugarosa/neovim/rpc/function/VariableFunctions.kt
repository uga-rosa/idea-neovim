package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun getChangedTick(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Int? =
    client.request("nvim_buf_get_var", listOf(bufferId, "changedtick"))
        ?.decode { it.asIntegerValue().toInt() }
