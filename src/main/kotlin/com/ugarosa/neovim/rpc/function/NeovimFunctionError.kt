package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

sealed interface NeovimFunctionError {
    data object Timeout : NeovimFunctionError

    data object IO : NeovimFunctionError

    data object BadRequest : NeovimFunctionError

    data object ResponseTypeMismatch : NeovimFunctionError

    data object Unexpected : NeovimFunctionError
}

@JvmName("translateResponse")
fun Either<NeovimRpcClient.RequestError, NeovimRpcClient.Response>.translate(): Either<NeovimFunctionError, Value> =
    either {
        val response = this@translate.mapLeft { it.translate() }.bind()
        ensure(response.error.isNilValue) {
            logger.warn("Neovim returned an error: ${response.error}")
            NeovimFunctionError.Unexpected
        }
        response.result
    }

private fun NeovimRpcClient.RequestError.translate(): NeovimFunctionError {
    return when (this) {
        is NeovimRpcClient.RequestError.BadRequest -> NeovimFunctionError.BadRequest
        is NeovimRpcClient.RequestError.IO -> NeovimFunctionError.IO
        is NeovimRpcClient.RequestError.Timeout -> NeovimFunctionError.Timeout
        is NeovimRpcClient.RequestError.Unexpected -> NeovimFunctionError.Unexpected
    }
}

@JvmName("translateNotify")
fun Either<NeovimRpcClient.NotifyError, Unit>.translate(): Either<NeovimFunctionError, Unit> = this.mapLeft { it.translate() }

private fun NeovimRpcClient.NotifyError.translate(): NeovimFunctionError {
    return when (this) {
        is NeovimRpcClient.NotifyError.BadRequest -> NeovimFunctionError.BadRequest
        is NeovimRpcClient.NotifyError.IO -> NeovimFunctionError.IO
        is NeovimRpcClient.NotifyError.Unexpected -> NeovimFunctionError.Unexpected
    }
}
