package com.ugarosa.neovim.rpc.client.event

import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.rpc.client.NeovimClient

fun NeovimClient.onModeChangeEventCustom() {
    onEvent("nvim_mode_change_event") { params ->
        val mode = NeovimMode.fromMode(params[0].asStr().str)
        getAndSetMode(mode)
    }
}
