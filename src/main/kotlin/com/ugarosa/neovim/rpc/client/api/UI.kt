package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.uiAttach() {
    request(
        "nvim_ui_attach",
        listOf(
            80,
            40,
            mapOf(
                "ext_multigrid" to true,
                "ext_cmdline" to true,
                "ext_messages" to true,
            ),
        ),
    )
}
