package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun hookModeChange(client: NeovimRpcClient): Either<NeovimFunctionError, Unit> =
    either {
        val chanId = getChanId(client).bind()
        val luaCode = readLuaCode("/lua/hookModeChange.lua")
        execLua(client, luaCode, listOf(chanId)).bind()
    }
