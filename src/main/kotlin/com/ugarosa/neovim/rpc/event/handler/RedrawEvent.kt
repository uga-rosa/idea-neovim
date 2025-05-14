package com.ugarosa.neovim.rpc.event.handler

import com.ugarosa.neovim.logger.MyLogger
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.event.handler.redraw.onCmdlineEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onHighlightEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onMessageEvent
import com.ugarosa.neovim.rpc.event.handler.redraw.onModeChangeEvent
import com.ugarosa.neovim.rpc.transport.NeovimObject

data class RedrawEvent(
    val name: String,
    val param: List<NeovimObject>,
)

private val logger = MyLogger.getInstance("com.ugarosa.neovim.rpc.event.handler")

fun onRedrawEvent(client: NeovimClient) {
    client.register("redraw") { rawBatches ->
        // rawBatches: List<…> = [ ['grid_resize', [2,77,36]], ['msg_showmode',[[]]], … ]
        rawBatches.forEach { rawBatch ->
            // batch: List<NeovimObject> = ['grid_resize', [2,77,36]]
            val batch = rawBatch.asArray()
            logger.trace("Redraw event: $batch")

            // Some events have a single parameter, while others have several parameters
            // e.g. ["hl_attr_define", [param1], [param2], [param3], ...]
            val name = batch[0].asString()
            batch.drop(1).forEach { param ->
                val redraw = RedrawEvent(name, param.asArray())
                onHighlightEvent(redraw)
                onCmdlineEvent(redraw)
                onModeChangeEvent(redraw)
                onMessageEvent(redraw)
            }
        }
    }
}
