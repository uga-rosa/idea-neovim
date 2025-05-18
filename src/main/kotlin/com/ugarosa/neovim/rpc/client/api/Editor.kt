package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimPosition
import com.ugarosa.neovim.window.WindowId

suspend fun NeovimClient.input(key: String) {
    notify("nvim_input", listOf(key))
}

suspend fun NeovimClient.setCursor(
    windowId: WindowId,
    pos: NeovimPosition,
) {
    execLuaNotify("window", "cursor", listOf(windowId, pos.line, pos.col, pos.curswant))
}
