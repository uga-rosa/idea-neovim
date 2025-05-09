package com.ugarosa.neovim.rpc.function

import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

val logger = Logger.getInstance("com.ugarosa.neovim.rpc.function")

fun NeovimRpcClient.Response.unpack(): Value? =
    if (error.isNilValue) {
        result
    } else {
        logger.warn("Response has error: $error")
        null
    }

fun <T> NeovimRpcClient.Response.decode(f: (Value) -> T): T? = unpack()?.decode(f)

suspend fun execLua(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): NeovimRpcClient.Response? = client.request("nvim_exec_lua", listOf(code, args))

suspend fun execLuaNotify(
    client: NeovimRpcClient,
    code: String,
    args: List<Any> = emptyList(),
): Unit = client.notify("nvim_exec_lua", listOf(code, args))

fun readLuaCode(resourcePath: String): String? =
    object {}.javaClass.getResource(resourcePath)?.readText()
        ?: run {
            logger.warn("Lua script not found: $resourcePath")
            null
        }
