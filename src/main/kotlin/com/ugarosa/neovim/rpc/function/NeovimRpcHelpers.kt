package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.MapValue
import org.msgpack.value.Value

val logger = Logger.getInstance("com.ugarosa.neovim.rpc.function")

suspend fun execLua(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): Either<NeovimFunctionError, Value> =
    client.request("nvim_exec_lua", listOf(code, args))
        .translate()

suspend fun execLuaNotify(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): Either<NeovimFunctionError, Unit> =
    client.notify("nvim_exec_lua", listOf(code, args))
        .translate()

fun Raise<NeovimFunctionError>.readLuaCode(resourcePath: String): String =
    object {}.javaClass.getResource(resourcePath)?.readText()
        ?: run {
            logger.warn("Lua script not found: $resourcePath")
            raise(NeovimFunctionError.Unexpected)
        }

suspend fun getChanId(client: NeovimRpcClient): Either<NeovimFunctionError, Int> =
    either {
        val result =
            client.request("nvim_get_chan_info", listOf(0))
                .translate().bind()
        Either.catch {
            result.asMapValue().get("id")?.asIntegerValue()?.toInt()!!
        }
            .mapLeft { NeovimFunctionError.ResponseTypeMismatch }.bind()
    }

private fun MapValue.get(key: String): Value? =
    this.map().entries
        .firstOrNull { it.key.isStringValue && it.key.asStringValue().asString() == key }
        ?.value
