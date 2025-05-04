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

suspend fun getCursor(client: NeovimRpcClient): Either<NeovimFunctionsError, Pair<Int, Int>> =
    either {
        val result =
            client.requestAsync("nvim_win_get_cursor", listOf(0))
                .translate().bind()
        Either.catch {
            val cursorArray = result.asArrayValue().list()
            val row = cursorArray[0].asIntegerValue().toInt()
            val col = cursorArray[1].asIntegerValue().toInt()
            row to col
        }
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
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
