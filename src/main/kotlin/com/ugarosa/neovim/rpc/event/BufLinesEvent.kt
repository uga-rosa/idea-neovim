package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.asBufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

data class BufLinesEvent(
    val bufferId: BufferId,
    val changedTick: Int,
    val firstLine: Int,
    val lastLine: Int,
    val replacementLines: List<String>,
)

fun maybeBufLinesEvent(push: NeovimRpcClient.PushNotification): BufLinesEvent? {
    if (push.method != "nvim_buf_lines_event") {
        return null
    }
    return push.params.decode {
        val params = it.asArrayValue().list()
        val bufferId = params[0].asBufferId()
        val changedTick = params[1].asIntegerValue().toInt()
        val firstLine = params[2].asIntegerValue().toInt()
        val lastLine = params[3].asIntegerValue().toInt()
        val replacementLines = params[4].asArrayValue().list().map { it.asStringValue().asString() }
        BufLinesEvent(bufferId, changedTick, firstLine, lastLine, replacementLines)
    }
}
