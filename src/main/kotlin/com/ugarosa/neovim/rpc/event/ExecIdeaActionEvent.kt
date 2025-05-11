package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify

data class ExecIdeaActionEvent(
    val bufferId: BufferId,
    val actionId: String,
)

suspend fun createExecIdeaActionCommand(client: NeovimRpcClient) {
    val chanId = ChanIdManager.get()
    execLuaNotify(client, "command", "create_exec_action", listOf(chanId))
}

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
