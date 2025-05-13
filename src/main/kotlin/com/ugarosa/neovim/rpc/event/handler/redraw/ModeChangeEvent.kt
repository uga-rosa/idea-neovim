package com.ugarosa.neovim.rpc.event.handler.redraw

import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent

fun maybeModeChangeEvent(redraw: RedrawEvent): NeovimMode? {
    if (redraw.name != "mode_change") {
        return null
    }

    val list = redraw.param.asArray()
    val mode = list[0].asString().let { NeovimMode.fromModeChangeEvent(it) }
    return mode
}
