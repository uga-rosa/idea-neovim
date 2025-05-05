package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.asBufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun createBuffer(client: NeovimRpcClient): Either<NeovimFunctionError, BufferId> =
    either {
        client.request("nvim_create_buf", listOf(true, false))
            .translate().bind()
            .asBufferId()
            .mapLeft { NeovimFunctionError.ResponseTypeMismatch }.bind()
    }

suspend fun setCurrentBuffer(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    either {
        client.request("nvim_set_current_buf", listOf(bufferId))
            .translate().bind()
    }

suspend fun bufferSetLines(
    client: NeovimRpcClient,
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
): Either<NeovimFunctionError, Unit> =
    either {
        client.request("nvim_buf_set_lines", listOf(bufferId, start, end, false, lines))
            .translate().bind()
    }

suspend fun bufferAttach(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    either {
        client.request("nvim_buf_attach", listOf(bufferId, false, emptyMap<String, Any>()))
            .translate().bind()
    }

suspend fun bufferDetach(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    either {
        client.notify("nvim_buf_detach", listOf(bufferId))
            .translate().bind()
    }
