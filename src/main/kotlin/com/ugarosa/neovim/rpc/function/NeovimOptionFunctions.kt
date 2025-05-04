package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.asAny

suspend fun getGlobalOptions(client: NeovimRpcClient): Either<NeovimFunctionsError, Map<String, Any>> =
    either {
        val luaCode = readLuaCode("/lua/getGlobalOptions.lua")
        try {
            execLua(client, luaCode).bind()
                .asMapValue().map()
                .mapKeys { it.key.asStringValue().asString() }
                .mapValues { it.value.asAny() }
        } catch (e: Exception) {
            logger.warn("Error executing Lua script: ${e.message}")
            raise(NeovimFunctionsError.Unexpected)
        }
    }

suspend fun getLocalOptions(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionsError, Map<String, Any>> =
    either {
        val luaCode = readLuaCode("/lua/getLocalOptions.lua")
        try {
            execLua(client, luaCode, listOf(bufferId)).bind()
                .asMapValue().map()
                .mapKeys { it.key.asStringValue().asString() }
                .mapValues { it.value.asAny() }
        } catch (e: Exception) {
            logger.warn("Error executing Lua script: ${e.message}")
            raise(NeovimFunctionsError.Unexpected)
        }
    }

suspend fun hookGlobalOptionSet(client: NeovimRpcClient): Either<NeovimFunctionsError, Unit> =
    either {
        val chanId = getChanId(client).bind()
        val luaCode = readLuaCode("/lua/hookGlobalOptionSet.lua")
        execLua(client, luaCode, listOf(chanId)).bind()
    }

suspend fun hookLocalOptionSet(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionsError, Unit> =
    either {
        val chanId = getChanId(client).bind()
        val luaCode = readLuaCode("/lua/hookLocalOptionSet.lua")
        execLua(client, luaCode, listOf(chanId, bufferId)).bind()
    }
