package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.action.NeovimActionManager
import com.ugarosa.neovim.buffer.NeovimBufferManager
import com.ugarosa.neovim.rpc.client.NeovimClient

fun onExecIdeaActionEvent(client: NeovimClient) {
    client.register("nvim_exec_idea_action_event") { params ->
        val bufferId = params[0].asBufferId()
        val actionId = params[1].asString()

        val editor = NeovimBufferManager.editor(bufferId)
        val actionManager = service<NeovimActionManager>()
        actionManager.executeAction(actionId, editor)
    }
}
