package com.ugarosa.neovim.rpc.client

import kotlinx.coroutines.CoroutineScope
import org.msgpack.value.Value

interface NeovimRpcClient {
    val scope: CoroutineScope

    suspend fun request(
        method: String,
        params: List<Any?> = emptyList(),
    ): Response?

    data class Response(
        val msgId: Int,
        val error: Value,
        val result: Value,
    )

    suspend fun notify(
        method: String,
        params: List<Any?> = emptyList(),
    )

    fun registerPushHandler(handler: suspend (PushNotification) -> Unit)

    data class PushNotification(
        val method: String,
        val params: Value,
    )
}
