package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.NeovimPosition
import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify
import com.ugarosa.neovim.rpc.function.readLuaCode

data class VisualSelectionEvent(
    val bufferId: BufferId,
    val mode: VisualMode,
    val startPosition: NeovimPosition,
    val endPosition: NeovimPosition,
)

enum class VisualMode(val str: String) {
    VISUAL("v"),
    VISUAL_LINE("V"),
    VISUAL_BLOCK("\u0016"), // Ctrl-V
    ;

    companion object {
        private val map = entries.associateBy { it.str }

        fun fromString(str: String): VisualMode = map[str] ?: throw IllegalArgumentException("Unknown visual mode: $str")
    }
}

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
        val mode = params[1].asStringValue().asString().let { VisualMode.fromString(it) }
        val startRow = params[2].asIntegerValue().toInt()
        val startCol = params[3].asIntegerValue().toInt()
        val endRow = params[4].asIntegerValue().toInt()
        val endCol = params[5].asIntegerValue().toInt()
        VisualSelectionEvent(bufferId, mode, NeovimPosition(startRow, startCol), NeovimPosition(endRow, endCol))
    }
}
