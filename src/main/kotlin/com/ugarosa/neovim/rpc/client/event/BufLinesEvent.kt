package com.ugarosa.neovim.rpc.client.event

import com.intellij.openapi.components.service
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimObject
import com.ugarosa.neovim.session.NeovimSessionManager

data class BufLinesEvent(
    val bufferId: NeovimObject.BufferId,
    val changedTick: Long,
    val firstLine: Int,
    val lastLine: Int,
    val replacementLines: List<String>,
)

fun NeovimClient.onBufLinesEvent() {
    onEvent("nvim_buf_lines_event") { params ->
        val bufferId = params[0].asBufferId()
        val changedTick = params[1].asInt64().long
        val firstLine = params[2].asInt64().long.toInt()
        val lastLine = params[3].asInt64().long.toInt()
        val replacementLines = params[4].asArray().list.map { it.asStr().str }
        val event = BufLinesEvent(bufferId, changedTick, firstLine, lastLine, replacementLines)

        val session = service<NeovimSessionManager>().getSession(bufferId)
        session.handleBufferLinesEvent(event)
    }
}
