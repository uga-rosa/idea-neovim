package com.ugarosa.neovim.adapter.nvim.outgoing

import com.intellij.openapi.components.service
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.setCursor

class CursorCommandAdapter {
    private val client = service<NvimClient>()

    suspend fun send(
        bufferId: BufferId,
        pos: NvimPosition,
    ) {
        client.setCursor(bufferId, pos)
    }
}
