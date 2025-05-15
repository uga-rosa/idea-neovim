package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimPosition
import com.ugarosa.neovim.session.NeovimSessionManager

data class CursorMoveEvent(
    val bufferId: BufferId,
    val position: NeovimPosition,
)

fun onCursorMoveEvent(client: NeovimClient) {
    client.register("nvim_cursor_move_event") { params ->
        val bufferId = params[0].asBufferId()
        val line = params[1].asInt()
        val col = params[2].asInt()
        val curswant = params[3].asInt()
        val event = CursorMoveEvent(bufferId, NeovimPosition(line, col, curswant))

        val session = service<NeovimSessionManager>().getSession(bufferId)
        session.handleCursorMoveEvent(event)
    }
}
