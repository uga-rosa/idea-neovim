package com.ugarosa.neovim.adapter.nvim.incoming

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.ugarosa.neovim.adapter.nvim.incoming.redraw.installRedrawAdapter
import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.rpc.client.NvimClient

@Service(Service.Level.APP)
class IncomingEventsRegistry {
    private val client = service<NvimClient>()
    private val bus = service<NvimToIdeaBus>()
    private val optionManager = service<NvimOptionManager>()

    init {
        installBufLinesAdapter(client, bus)
        installCursorAdapter(client, bus)
        installModeAdapter(client, bus)
        installVisualSelectionAdapter(client, bus)
        installOptionAdapter(client, optionManager)
        installActionAdapter(client)
        installRedrawAdapter(client)
    }
}
