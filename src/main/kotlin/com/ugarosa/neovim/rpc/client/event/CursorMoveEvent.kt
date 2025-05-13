package com.ugarosa.neovim.rpc.client.event

import com.intellij.openapi.components.service
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimObject
import com.ugarosa.neovim.rpc.type.NeovimPosition
import com.ugarosa.neovim.session.NeovimSessionManager

data class CursorMoveEvent(
    val bufferId: NeovimObject.BufferId,
    val position: NeovimPosition,
)

fun NeovimClient.onCursorMoveEvent() {
    onEvent("nvim_cursor_move_event") { params ->
        val bufferId = params[0].asBufferId()
        val lnum = params[1].asInt64().long.toInt()
        val col = params[2].asInt64().long.toInt()
        val curswant = params[3].asInt64().long.toInt()
        val event = CursorMoveEvent(bufferId, NeovimPosition(lnum, col, curswant))

        val session = service<NeovimSessionManager>().getSession(bufferId)
        session.handleCursorMoveEvent(event)
    }
}
