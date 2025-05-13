package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.domain.NeovimPosition
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

// Neovim cursor position is (1,0) byte-indexed
data class CursorMoveEvent(
    val bufferId: BufferId,
    val position: NeovimPosition,
)

fun maybeCursorMoveEvent(push: NeovimRpcClient.PushNotification): CursorMoveEvent? {
    if (push.method != "nvim_cursor_move_event") {
        return null
    }
    return push.params.decode { value ->
        val params = value.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val lnum = params[1].asIntegerValue().toInt()
        val col = params[2].asIntegerValue().toInt()
        val curswant = params[3].asIntegerValue().toInt()
        CursorMoveEvent(bufferId, NeovimPosition(lnum, col, curswant))
    }
}
