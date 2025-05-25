package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.bus.NvimCursorMoved
import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.rpc.client.NvimClient

fun installCursorAdapter(
    client: NvimClient,
    bus: NvimToIdeaBus,
) {
    client.register("IdeaNeovim:CursorMoved") { params ->
        val bufferId = params[0].asBufferId()
        val line = params[1].asInt()
        val col = params[2].asInt()
        val curswant = params[3].asInt()
        val pos = NvimPosition(line, col, curswant)
        val event = NvimCursorMoved(bufferId, pos)
        bus.tryEmit(event)
    }
}
