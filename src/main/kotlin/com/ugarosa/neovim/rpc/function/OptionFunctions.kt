package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.asAny
import org.msgpack.value.Value

suspend fun getGlobalOptions(client: NeovimRpcClient): Either<NeovimFunctionError, Map<String, Any>> =
    either {
        val luaCode = readLuaCode("/lua/getGlobalOptions.lua")
        execLua(client, luaCode).bind()
            .asStringMap().bind()
    }

suspend fun getLocalOptions(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Map<String, Any>> =
    either {
        val luaCode = readLuaCode("/lua/getLocalOptions.lua")
        execLua(client, luaCode, listOf(bufferId)).bind()
            .asStringMap().bind()
    }

private fun Value.asStringMap(): Either<NeovimFunctionError, Map<String, Any>> =
    Either.catch {
        this@asStringMap.asMapValue().map()
            .mapKeys { it.key.asStringValue().asString() }
            .mapValues { it.value.asAny() }
    }.mapLeft {
        logger.warn("Error converting Value to Map: ${it.message}")
        NeovimFunctionError.Unexpected
    }

suspend fun hookGlobalOptionSet(client: NeovimRpcClient): Either<NeovimFunctionError, Unit> =
    either {
        val chanId = ChanIdManager.fetch(client).bind()
        val luaCode = readLuaCode("/lua/hookGlobalOptionSet.lua")
        execLuaNotify(client, luaCode, listOf(chanId)).bind()
    }

suspend fun hookLocalOptionSet(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    either {
        val chanId = ChanIdManager.fetch(client).bind()
        val luaCode = readLuaCode("/lua/hookLocalOptionSet.lua")
        execLuaNotify(client, luaCode, listOf(chanId, bufferId)).bind()
    }
