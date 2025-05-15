package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.BufferId
import com.ugarosa.neovim.session.NeovimSessionManager

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

        val session = service<NeovimSessionManager>().getSession(bufferId)
        session.handleBufferLinesEvent(event)
    }
}
