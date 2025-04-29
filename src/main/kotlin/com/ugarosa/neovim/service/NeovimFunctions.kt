package com.ugarosa.neovim.service

import com.ugarosa.neovim.infra.NeovimRpcClient

object NeovimFunctions {
    fun createBuffer(rpcClient: NeovimRpcClient): BufferId {
        val response = rpcClient.requestSync("nvim_create_buf", listOf(true, false), null)
        return response.result.asBufferId()
    }

    fun bufferSetLines(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
        start: Int,
        end: Int,
        lines: List<String>,
    ) {
        rpcClient.requestSync(
            "nvim_buf_set_lines",
            listOf(bufferId, start, end, false, lines),
        )
    }

    fun bufferAttach(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestSync(
            "nvim_buf_attach",
            listOf(bufferId, true, emptyMap<String, Any>()),
        )
    }

    fun setCurrentBuffer(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestSync("nvim_set_current_buf", listOf(bufferId))
    }

    fun input(
        rpcClient: NeovimRpcClient,
        key: String,
    ) {
        rpcClient.requestSync("nvim_input", listOf(key))
    }

    fun getCursor(rpcClient: NeovimRpcClient): Pair<Int, Int> {
        val response = rpcClient.requestSync("nvim_win_get_cursor", listOf(0))
        val cursorArray = response.result.asArrayValue().list()
        val row = cursorArray[0].asIntegerValue().toInt()
        val col = cursorArray[1].asIntegerValue().toInt()
        return row to col
    }

    data class BufLinesEvent(
        val bufferId: BufferId,
        val firstLine: Int,
        val lastLine: Int,
        val replacementLines: List<String>,
    )

    fun maybeBufLinesEvent(push: NeovimRpcClient.PushNotification): BufLinesEvent? {
        if (push.method != "nvim_buf_lines_event") {
            return null
        }
        val params = push.params.asArrayValue().list()
        val bufferId = params[0].asBufferId()
        val firstLine = params[2].asIntegerValue().toInt()
        val lastLine = params[3].asIntegerValue().toInt()
        val replacementLines = params[4].asArrayValue().list().map { it.asStringValue().asString() }
        return BufLinesEvent(bufferId, firstLine, lastLine, replacementLines)
    }
}
