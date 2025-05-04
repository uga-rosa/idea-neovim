package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun input(
    client: NeovimRpcClient,
    key: String,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync("nvim_input", listOf(key))
            .translate().bind()
    }

suspend fun setCursor(
    client: NeovimRpcClient,
    pos: Pair<Int, Int>,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_win_set_cursor",
            listOf(0, listOf(pos.first, pos.second)),
        )
            .translate().bind()
    }
