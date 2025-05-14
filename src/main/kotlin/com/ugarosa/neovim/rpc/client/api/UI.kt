package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient

suspend fun NeovimClient.uiAttach() {
    notify(
        "nvim_ui_attach",
        listOf(
            80,
            40,
            mapOf(
                "ext_cmdline" to true,
                "ext_messages" to true,
            ),
        ),
    )
}
