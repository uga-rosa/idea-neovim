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

suspend fun hookCursorMove(client: NeovimRpcClient): Either<NeovimFunctionsError, Unit> =
    either {
        val chanId = getChanId(client).bind()
        val luaCode = readLuaCode("/lua/hookCursorMove.lua")
        execLua(client, luaCode, listOf(chanId)).bind()
    }

suspend fun setCursor(
    client: NeovimRpcClient,
    row: Int,
    col: Int,
): Either<NeovimFunctionsError, Unit> =
    either {
        val luaCode = readLuaCode("/lua/setCursorWithoutEvent.lua")
        execLua(client, luaCode, listOf(row, col)).bind()
    }
