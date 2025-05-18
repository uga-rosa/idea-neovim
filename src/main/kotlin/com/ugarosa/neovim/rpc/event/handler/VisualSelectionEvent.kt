package com.ugarosa.neovim.rpc.event.handler

import com.ugarosa.neovim.buffer.NeovimBufferManager
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimRegion

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

        val buffer = NeovimBufferManager.findById(bufferId)
        buffer.handleVisualSelectionEvent(regions)
    }
}
