package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.logger.MyLogger
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

val logger = MyLogger.getInstance("com.ugarosa.neovim.rpc.function")

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
    packageName: String,
    method: String,
    args: List<Any> = emptyList(),
): NeovimRpcClient.Response? {
    val code = "return require('intellij.$packageName').$method(...)"
    return client.request("nvim_exec_lua", listOf(code, args))
}

suspend fun execLuaNotify(
    client: NeovimRpcClient,
    packageName: String,
    method: String,
    args: List<Any> = emptyList(),
) {
    val code = "require('intellij.$packageName').$method(...)"
    client.notify("nvim_exec_lua", listOf(code, args))
}
