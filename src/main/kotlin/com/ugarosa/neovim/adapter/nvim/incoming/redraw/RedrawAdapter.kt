package com.ugarosa.neovim.adapter.nvim.incoming.redraw

import com.ugarosa.neovim.rpc.client.NvimClient

fun installRedrawAdapter(client: NvimClient) {
    client.register("redraw") { rawBatches ->
        // rawBatches: List<…> = [ ['grid_resize', [2,77,36]], ['msg_showmode',[[]]], … ]
        rawBatches.forEach { rawBatch ->
            // batch: List<NeovimObject> = ['grid_resize', [2,77,36]]
            val batch = rawBatch.asArray()

            // Some events have a single parameter, while others have several parameters
            // e.g. ["hl_attr_define", [param1], [param2], [param3], ...]
            val name = batch[0].asString()
            batch.drop(1).forEach { rawParam ->
                val param = rawParam.asArray()
                onHighlightEvent(name, param)
                onCmdlineEvent(name, param)
                onMessageEvent(name, param)
            }
        }
    }
}
