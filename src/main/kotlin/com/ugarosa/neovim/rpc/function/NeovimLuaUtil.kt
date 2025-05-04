package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
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
                ?: throw IllegalStateException("Lua script not found")
        execLua(client, luaCode)
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
