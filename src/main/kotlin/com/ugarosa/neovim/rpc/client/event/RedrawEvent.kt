package com.ugarosa.neovim.rpc.client.event

import com.intellij.openapi.components.service
import com.ugarosa.neovim.cmdline.NeovimCmdlineManager
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.event.redraw.maybeCmdlineEvent
import com.ugarosa.neovim.rpc.client.event.redraw.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.type.NeovimObject
import com.ugarosa.neovim.session.NeovimSessionManager
import com.ugarosa.neovim.statusline.StatusLineManager

data class RedrawEvent(
    val name: String,
    val param: NeovimObject,
)

fun NeovimClient.onRedrawEvent() {
    onEvent("redraw") { params ->
        params.forEach {
            val elem = it.asArray().list
            val redraw = RedrawEvent(elem[0].asStr().str, elem[1])

            maybeCmdlineEvent(redraw)?.let { event ->
                service<NeovimCmdlineManager>().handleEvent(event)
            }

            maybeModeChangeEvent(redraw)?.let { mode ->
                service<NeovimSessionManager>().getSession()?.handleModeChangeEvent(mode)

                focusProject()?.service<StatusLineManager>()?.updateStatusLine(mode)

                if (!mode.isCommand()) {
                    service<NeovimCmdlineManager>().destroy()
                }
            }
        }
    }
}
