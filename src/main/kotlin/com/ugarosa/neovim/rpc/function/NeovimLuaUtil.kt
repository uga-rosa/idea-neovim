package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.asAny
import org.msgpack.value.Value

private suspend fun execLua(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): Either<NeovimFunctionsError, Value> =
    either {
        client.requestAsync(
            "nvim_exec_lua",
            listOf(code, args),
        )
            .translate().bind()
    }

suspend fun enforceSingleWindow(client: NeovimRpcClient): Either<NeovimFunctionsError, Unit> =
    either {
        val luaCode =
            object {}.javaClass.getResource("/lua/enforceSingleWindow.lua")?.readText()
                ?: run {
                    logger.warn("Lua script not found: /enforceSingleWindow.lua")
                    raise(NeovimFunctionsError.Unexpected)
                }
        execLua(client, luaCode).bind()
    }

suspend fun getGlobalOptions(client: NeovimRpcClient): Either<NeovimFunctionsError, Map<String, Any>> =
    either {
        val luaCode =
            object {}.javaClass.getResource("/lua/getGlobalOptions.lua")?.readText()
                ?: run {
                    logger.warn("Lua script not found: /getGlobalOptions.lua")
                    raise(NeovimFunctionsError.Unexpected)
                }
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
        val luaCode =
            object {}.javaClass.getResource("/lua/getLocalOptions.lua")?.readText()
                ?: run {
                    logger.warn("Lua script not found: /getLocalOptions.lua")
                    raise(NeovimFunctionsError.Unexpected)
                }
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

private suspend fun getChanId(client: NeovimRpcClient): Either<NeovimFunctionsError, Int> =
    either {
        val response =
            client.requestAsync("nvim_get_chan_info", listOf(0))
                .mapLeft { it.translate() }.bind()
        Either.catch {
            response.result.asMapValue().get("id")?.asIntegerValue()?.toInt()!!
        }
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
    }
