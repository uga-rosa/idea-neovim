package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient

suspend fun NeovimClient.uiAttach() {
    connectionManager.notify(
        "nvim_ui_attach",
        listOf(
            80,
            40,
            mapOf(
                "rgb" to true,
                "ext_cmdline" to true,
            ),
        ),
    )
}
