package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

sealed interface NeovimFunctionsError {
    data object Timeout : NeovimFunctionsError

    data object IO : NeovimFunctionsError

    data object BadRequest : NeovimFunctionsError

    data object ResponseTypeMismatch : NeovimFunctionsError

    data object Unexpected : NeovimFunctionsError
}

fun NeovimRpcClient.RequestError.translate(): NeovimFunctionsError {
    return when (this) {
        is NeovimRpcClient.RequestError.BadRequest -> NeovimFunctionsError.BadRequest
        is NeovimRpcClient.RequestError.IO -> NeovimFunctionsError.IO
        is NeovimRpcClient.RequestError.Timeout -> NeovimFunctionsError.Timeout
        is NeovimRpcClient.RequestError.Unexpected -> NeovimFunctionsError.Unexpected
    }
}
