package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun getChangedTick(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Int> =
    client.request("nvim_buf_get_var", listOf(bufferId, "changedtick"))
        .translate()
        .flatMapValue { it.asIntegerValue().toInt() }
