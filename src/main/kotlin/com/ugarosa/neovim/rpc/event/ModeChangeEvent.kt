package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

data class ModeChangeEvent(
    val bufferId: BufferId,
    val mode: NeovimMode,
)

fun maybeModeChangeEvent(push: NeovimRpcClient.PushNotification): ModeChangeEvent? {
    if (push.method != "nvim_mode_change_event") {
        return null
    }
    return push.params.decode {
        val params = it.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val mode = params[1].asStringValue().asString().let { NeovimMode.fromRaw(it) }
        ModeChangeEvent(bufferId, mode)
    }
}
