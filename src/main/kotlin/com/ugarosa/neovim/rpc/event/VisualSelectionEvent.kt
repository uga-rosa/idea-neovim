package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.domain.NeovimRegion
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify
import com.ugarosa.neovim.rpc.function.readLuaCode

data class VisualSelectionEvent(
    val bufferId: BufferId,
    val regions: List<NeovimRegion>,
)

suspend fun hookVisualSelectionEvent(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookVisualSelectionEvent.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}

fun maybeVisualSelectionEvent(push: NeovimRpcClient.PushNotification): VisualSelectionEvent? {
    if (push.method != "nvim_visual_selection_event") {
        return null
    }
    return push.params.decode { value ->
        val params = value.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val regions =
            params[1].asArrayValue().list().map {
                val list = it.asArrayValue().list()
                val row = list[0].asIntegerValue().toInt()
                val startColumn = list[1].asIntegerValue().toInt()
                val endColumn = list[2].asIntegerValue().toInt()
                NeovimRegion(row, startColumn, endColumn)
            }
        VisualSelectionEvent(bufferId, regions)
    }
}
