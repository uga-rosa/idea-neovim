package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.Raise
import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

val logger = Logger.getInstance("com.ugarosa.neovim.rpc.function")

suspend fun execLua(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): Either<NeovimFunctionError, Value> =
    client.request("nvim_exec_lua", listOf(code, args))
        .translate()

fun Raise<NeovimFunctionError>.readLuaCode(resourcePath: String): String =
    object {}.javaClass.getResource(resourcePath)?.readText()
        ?: run {
            logger.warn("Lua script not found: $resourcePath")
            raise(NeovimFunctionError.Unexpected)
        }
