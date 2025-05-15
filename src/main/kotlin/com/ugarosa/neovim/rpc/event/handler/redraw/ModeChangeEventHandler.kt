package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent
import com.ugarosa.neovim.session.NeovimSessionManager

suspend fun onModeChangeEvent(redraw: RedrawEvent) {
    when (redraw.name) {
        "mode_change" -> {
            val mode = redraw.param[0].asString().let { NeovimMode.fromModeChangeEvent(it) }

            val session = service<NeovimSessionManager>().getSession()
            session?.handleModeChangeEvent(mode)
        }
    }
}
