package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

fun maybeModeChangeEventCustom(push: NeovimRpcClient.PushNotification): NeovimMode? {
    if (push.method != "nvim_mode_change_event") {
        return null
    }
    return push.params.decode {
        val params = it.asArrayValue().list()
        val mode = params[0].asStringValue().asString()
        NeovimMode.fromMode(mode)
    }
}
