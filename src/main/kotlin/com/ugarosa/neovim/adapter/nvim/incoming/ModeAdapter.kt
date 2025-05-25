package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.bus.ModeChanged
import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.domain.mode.NvimMode
import com.ugarosa.neovim.rpc.client.NvimClient

fun installModeAdapter(
    client: NvimClient,
    bus: NvimToIdeaBus,
) {
    client.register("IdeaNeovim:ModeChanged") { params ->
        val bufferId = params[0].asBufferId()
        val mode = NvimMode.fromMode(params[1].asString()) ?: return@register
        val event = ModeChanged(bufferId, mode)
        bus.tryEmit(event)
    }
}
