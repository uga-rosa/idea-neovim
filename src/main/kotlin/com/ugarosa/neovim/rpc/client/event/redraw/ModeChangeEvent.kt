package com.ugarosa.neovim.rpc.client.event.redraw

import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.client.event.RedrawEvent

fun maybeModeChangeEvent(redraw: RedrawEvent): NeovimMode? {
    if (redraw.name != "mode_change") {
        return null
    }

    val list = redraw.param.asArray().list
    val mode = list[0].asStr().str.let { NeovimMode.fromModeChangeEvent(it) }
    return mode
}
