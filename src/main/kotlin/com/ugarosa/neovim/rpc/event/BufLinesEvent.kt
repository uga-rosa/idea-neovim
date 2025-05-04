package com.ugarosa.neovim.rpc.event

import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.asBufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

private val logger = Logger.getInstance("com.ugarosa.neovim.rpc.event")

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
    try {
        val params = push.params.asArrayValue().list()
        val bufferId =
            params[0].asBufferId().getOrNull()
                ?: throw IllegalArgumentException("Invalid buffer ID: ${params[0]}")
        val firstLine = params[2].asIntegerValue().toInt()
        val lastLine = params[3].asIntegerValue().toInt()
        val replacementLines = params[4].asArrayValue().list().map { it.asStringValue().asString() }
        return BufLinesEvent(bufferId, firstLine, lastLine, replacementLines)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
