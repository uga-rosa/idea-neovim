package com.ugarosa.neovim.rpc.event.handler

import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.rpc.client.NeovimClient

fun onModeChangeEventCustom(client: NeovimClient) {
    client.register("nvim_mode_change_event") { params ->
        val mode = NeovimMode.fromMode(params[0].asString())
        getAndSetMode(mode)
    }
}
