package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.action.NeovimActionManager
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.session.NeovimSessionManager

fun onExecIdeaActionEvent(client: NeovimClient) {
    client.register("nvim_exec_idea_action_event") { params ->
        val bufferId = params[0].asBufferId()
        val actionId = params[1].asStr().str

        val editor = service<NeovimSessionManager>().getEditor(bufferId)
        val actionManager = service<NeovimActionManager>()
        actionManager.executeAction(actionId, editor)
    }
}
