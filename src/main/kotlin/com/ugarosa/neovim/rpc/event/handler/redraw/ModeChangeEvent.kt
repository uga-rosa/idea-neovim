package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.cmdline.NeovimCmdlineManager
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent
import com.ugarosa.neovim.session.NeovimSessionManager
import com.ugarosa.neovim.statusline.StatusLineManager

suspend fun onModeChangeEvent(redraw: RedrawEvent) {
    if (redraw.name != "mode_change") {
        return
    }

    val list = redraw.param.asArray()
    val mode = list[0].asString().let { NeovimMode.fromModeChangeEvent(it) }

    service<NeovimSessionManager>().getSession()?.handleModeChangeEvent(mode)

    focusProject()?.service<StatusLineManager>()?.updateStatusLine(mode)

    if (!mode.isCommand()) {
        service<NeovimCmdlineManager>().destroy()
    }
}
