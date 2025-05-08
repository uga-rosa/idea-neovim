package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

data class ExecIdeaActionEvent(
    val bufferId: BufferId,
    val actionId: String,
)

fun maybeExecIdeaActionEvent(push: NeovimRpcClient.PushNotification): ExecIdeaActionEvent? {
    if (push.method != "nvim_exec_idea_action_event") {
        return null
    }
    try {
        val params = push.params.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val actionId = params[1].asStringValue().asString()
        return ExecIdeaActionEvent(bufferId, actionId)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
