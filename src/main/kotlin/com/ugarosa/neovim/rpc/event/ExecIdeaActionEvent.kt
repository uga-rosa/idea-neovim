package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
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
    return push.params.decode {
        val params = it.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val actionId = params[1].asStringValue().asString()
        ExecIdeaActionEvent(bufferId, actionId)
    }
}
