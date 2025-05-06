package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun input(
    client: NeovimRpcClient,
    key: String,
): Either<NeovimFunctionError, Unit> =
    client.notify("nvim_input", listOf(key))
        .translate()

suspend fun hookCursorMove(client: NeovimRpcClient): Either<NeovimFunctionError, Unit> =
    either {
        val chanId = ChanIdManager.fetch(client).bind()
        val luaCode = readLuaCode("/lua/hookCursorMove.lua")
        execLuaNotify(client, luaCode, listOf(chanId)).bind()
    }

suspend fun setCursor(
    client: NeovimRpcClient,
    row: Int,
    col: Int,
): Either<NeovimFunctionError, Unit> =
    either {
        val luaCode = readLuaCode("/lua/setCursorWithoutEvent.lua")
        execLuaNotify(client, luaCode, listOf(row, col)).bind()
    }
