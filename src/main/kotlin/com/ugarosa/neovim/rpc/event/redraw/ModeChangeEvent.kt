package com.ugarosa.neovim.rpc.event.redraw

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.mode.NeovimMode

data class ModeChangeEvent(
    val mode: NeovimMode,
)

fun maybeModeChangeEvent(event: RedrawEvent): ModeChangeEvent? {
    if (event.name != "mode_change") {
        return null
    }
    return event.param.decode {
        val params = it.asArrayValue().list()
        val mode = params[0].asStringValue().asString().let { NeovimMode.fromRaw(it) }
        ModeChangeEvent(mode)
    }
}
