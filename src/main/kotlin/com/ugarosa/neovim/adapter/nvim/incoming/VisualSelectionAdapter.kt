package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.bus.VisualSelectionChanged
import com.ugarosa.neovim.domain.position.NvimRegion
import com.ugarosa.neovim.rpc.client.NvimClient

fun installVisualSelectionAdapter(
    client: NvimClient,
    bus: NvimToIdeaBus,
) {
    client.register("IdeaNeovim:VisualSelection") { params ->
        val bufferId = params[0].asBufferId()
        val regions =
            params[1].asArray().map {
                val list = it.asArray()
                val row = list[0].asInt()
                val startColumn = list[1].asInt()
                val endColumn = list[2].asInt()
                NvimRegion(row, startColumn, endColumn)
            }
        val event = VisualSelectionChanged(bufferId, regions)
        bus.tryEmit(event)
    }
}
