package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimRegion
import com.ugarosa.neovim.session.NeovimSessionManager

fun onVisualSelectionEvent(client: NeovimClient) {
    client.register("nvim_visual_selection_event") { params ->
        val bufferId = params[0].asBufferId()
        val regions =
            params[1].asArray().map {
                val list = it.asArray()
                val row = list[0].asInt()
                val startColumn = list[1].asInt()
                val endColumn = list[2].asInt()
                NeovimRegion(row, startColumn, endColumn)
            }

        val session = service<NeovimSessionManager>().getSession(bufferId)
        session.handleVisualSelectionEvent(regions)
    }
}
