package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun uiAttach(client: NeovimRpcClient) {
    client.notify(
        "nvim_ui_attach",
        listOf(
            1000,
            1000,
            mapOf(
                "rgb" to true,
                "ext_cmdline" to true,
            ),
        ),
    )
}
