package com.ugarosa.neovim.adapter.nvim.incoming

import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.rpc.client.NvimClient

fun installOptionAdapter(
    client: NvimClient,
    optionManager: NvimOptionManager,
) {
    client.register("IdeaNeovim:OptionSet") { params ->
        val bufferId = params[0].asBufferId()
        val scope = params[1].asString()
        val key = params[2].asString()
        val value = params[3].asAny()
        when (scope) {
            "global" -> optionManager.putGlobal(key, value)
            "local" -> optionManager.putLocal(bufferId, key, value)
        }
    }
}
