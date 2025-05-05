package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun enforceSingleWindow(client: NeovimRpcClient): Either<NeovimFunctionError, Unit> =
    either {
        val luaCode = readLuaCode("/lua/enforceSingleWindow.lua")
        execLua(client, luaCode).bind()
    }
