package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.intellij.openapi.diagnostic.Logger
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
