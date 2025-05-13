package com.ugarosa.neovim.rpc.event.handler

import com.intellij.openapi.components.service
import com.ugarosa.neovim.cmdline.NeovimCmdlineManager
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.logger.MyLogger
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.event.handler.redraw.maybeCmdlineEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onSetHighlight
import com.ugarosa.neovim.rpc.transport.NeovimObject
import com.ugarosa.neovim.session.NeovimSessionManager
import com.ugarosa.neovim.statusline.StatusLineManager

data class RedrawEvent(
    val name: String,
    val param: NeovimObject,
)

private val logger = MyLogger.getInstance("com.ugarosa.neovim.rpc.event.handler")

fun onRedrawEvent(client: NeovimClient) {
    client.register("redraw") { params ->
        params.forEach {
            logger.trace("Redraw event: $it")

            val elem = it.asArray()
            val redraw = RedrawEvent(elem[0].asString(), elem[1])

            onSetHighlight(redraw)

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
