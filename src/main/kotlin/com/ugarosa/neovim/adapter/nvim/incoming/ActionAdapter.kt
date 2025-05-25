package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.adapter.idea.action.executeAction
import com.ugarosa.neovim.rpc.client.NvimClient

fun installActionAdapter(client: NvimClient) {
    client.register("IdeaNeovim:ExecIdeaAction") { params ->
        val actionId = params[0].asString()
        executeAction(actionId)
    }
}
