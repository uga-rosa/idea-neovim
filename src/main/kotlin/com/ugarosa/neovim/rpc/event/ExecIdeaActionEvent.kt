package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify
import com.ugarosa.neovim.rpc.function.readLuaCode

data class ExecIdeaActionEvent(
    val bufferId: BufferId,
    val actionId: String,
)

suspend fun createExecIdeaActionCommand(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/createExecIdeaActionCommand.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
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
