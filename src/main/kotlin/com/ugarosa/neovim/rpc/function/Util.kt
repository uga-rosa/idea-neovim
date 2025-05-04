package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

val logger = Logger.getInstance("com.ugarosa.neovim.rpc.function")

fun Either<NeovimRpcClient.RequestError, NeovimRpcClient.Response>.translate(): Either<NeovimFunctionsError, Value> =
    either {
        val response = this@translate.mapLeft { it.translate() }.bind()
        ensure(response.error.isNilValue) {
            logger.warn("Neovim returned an error: ${response.error}")
            NeovimFunctionsError.Unexpected
        }
        response.result
    }

suspend fun execLua(
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

fun Raise<NeovimFunctionsError>.readLuaCode(resourcePath: String): String {
    return object {}.javaClass.getResource(resourcePath)?.readText()
        ?: run {
            logger.warn("Lua script not found: $resourcePath")
            raise(NeovimFunctionsError.Unexpected)
        }
}

suspend fun getChanId(client: NeovimRpcClient): Either<NeovimFunctionsError, Int> =
    either {
        val response =
            client.requestAsync("nvim_get_chan_info", listOf(0))
                .mapLeft { it.translate() }.bind()
        Either.catch {
            response.result.asMapValue().get("id")?.asIntegerValue()?.toInt()!!
        }
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
    }
