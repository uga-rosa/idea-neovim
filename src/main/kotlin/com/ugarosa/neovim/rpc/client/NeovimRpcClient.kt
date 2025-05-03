package com.ugarosa.neovim.rpc.client

import arrow.core.Either
import org.msgpack.value.Value

interface NeovimRpcClient {
    suspend fun requestAsync(
        method: String,
        params: List<Any?> = emptyList(),
        timeoutMills: Long? = 500,
    ): Either<RequestError, Response>

    data class Response(
        val msgId: Int,
        val error: Value,
        val result: Value,
    )

    sealed interface RequestError {
        data object BadRequest : RequestError

        data object IO : RequestError

        data object Timeout : RequestError

        data object Unexpected : RequestError
    }

    fun registerPushHandler(handler: suspend (PushNotification) -> Unit)

    data class PushNotification(
        val method: String,
        val params: Value,
    )
}
