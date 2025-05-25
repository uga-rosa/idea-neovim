package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.bus.NvimBufLines
import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.rpc.client.NvimClient

fun installBufLinesAdapter(
    client: NvimClient,
    bus: NvimToIdeaBus,
) {
    client.register("nvim_buf_lines_event") { params ->
        val bufferId = params[0].asBufferId()
        val changedTick = params[1].asLong()
        val firstLine = params[2].asInt()
        val lastLine = params[3].asInt()
        val replacementLines = params[4].asArray().map { it.asString() }
        val event = NvimBufLines(bufferId, changedTick, firstLine, lastLine, replacementLines)
        bus.tryEmit(event)
    }
}
