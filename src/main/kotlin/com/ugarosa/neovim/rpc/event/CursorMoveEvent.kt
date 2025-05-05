package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

// Neovim cursor position is (1,0) byte-indexed
data class CursorMoveEvent(
    val bufferId: BufferId,
    val line: Int,
    val column: Int,
)

fun maybeCursorMoveEvent(push: NeovimRpcClient.PushNotification): CursorMoveEvent? {
    if (push.method != "nvim_cursor_move_event") {
        return null
    }
    try {
        val params = push.params.asArrayValue().list()
        val bufferId =
            params[0].asIntegerValue().toInt()
                .let { BufferId(it) }
        val line = params[1].asIntegerValue().toInt()
        val column = params[2].asIntegerValue().toInt()
        return CursorMoveEvent(bufferId, line, column)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
