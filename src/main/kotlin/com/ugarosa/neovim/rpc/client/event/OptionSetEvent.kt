package com.ugarosa.neovim.rpc.client.event

import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.rpc.client.NeovimClient

fun NeovimClient.onOptionSetEvent() {
    onEvent("nvim_option_set_event") { params ->
        val bufferId = params[0].asBufferId()
        val scope = params[1].asStr().str
        val name = params[2].asStr().str
        val value = params[3].asAny()

        val optionManager = service<NeovimOptionManager>()
        when (scope) {
            "global" -> optionManager.putGlobal(name, value)
            "local" -> optionManager.putLocal(bufferId, name, value)
        }
    }
}
