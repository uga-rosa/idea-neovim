package com.ugarosa.neovim.rpc.event.handler

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.buffer.NeovimBufferManager
import com.ugarosa.neovim.rpc.client.NeovimClient

data class BufLinesEvent(
    val bufferId: BufferId,
    val changedTick: Long,
    // 0-indexed
    val firstLine: Int,
    // 0-indexed, exclusive
    val lastLine: Int,
    val replacementLines: List<String>,
)

fun onBufLinesEvent(client: NeovimClient) {
    client.register("nvim_buf_lines_event") { params ->
        val bufferId = params[0].asBufferId()
        val changedTick = params[1].asLong()
        val firstLine = params[2].asInt()
        val lastLine = params[3].asInt()
        val replacementLines = params[4].asArray().map { it.asString() }
        val event = BufLinesEvent(bufferId, changedTick, firstLine, lastLine, replacementLines)

        val buffer = NeovimBufferManager.findById(bufferId)
        buffer.onBufferLinesEvent(event)
    }
}
