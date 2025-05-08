package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.asBufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun createBuffer(client: NeovimRpcClient): Either<NeovimFunctionError, BufferId> =
    client.request("nvim_create_buf", listOf(true, false))
        .translate()
        .flatMapValue { it.asBufferId() }

suspend fun setCurrentBuffer(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    client.notify("nvim_set_current_buf", listOf(bufferId))
        .translate()

suspend fun bufferSetLines(
    client: NeovimRpcClient,
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
): Either<NeovimFunctionError, Unit> =
    client.notify("nvim_buf_set_lines", listOf(bufferId, start, end, false, lines))
        .translate()

data class BufferSetTextParams(
    val bufferId: BufferId,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val replacement: List<String>,
)

suspend fun bufferSetText(
    client: NeovimRpcClient,
    params: BufferSetTextParams,
): Either<NeovimFunctionError, Unit> =
    client.notify(
        "nvim_buf_set_text",
        listOf(
            params.bufferId,
            params.startRow,
            params.startCol,
            params.endRow,
            params.endCol,
            params.replacement,
        ),
    )
        .translate()

suspend fun bufferAttach(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Either<NeovimFunctionError, Unit> =
    client.notify("nvim_buf_attach", listOf(bufferId, false, emptyMap<String, Any>()))
        .translate()
