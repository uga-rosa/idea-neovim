package com.ugarosa.neovim.rpc.event.handler

import com.ugarosa.neovim.logger.MyLogger
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.event.handler.redraw.onCmdlineEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onHighlightEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onModeChangeEvent
import com.ugarosa.neovim.rpc.transport.NeovimObject

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

            onHighlightEvent(redraw)
            onCmdlineEvent(redraw)
            onModeChangeEvent(redraw)
        }
    }
}
