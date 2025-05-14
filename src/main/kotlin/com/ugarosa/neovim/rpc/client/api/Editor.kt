package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.BufferId
import com.ugarosa.neovim.rpc.type.NeovimPosition

suspend fun NeovimClient.input(key: String) {
    notify("nvim_input", listOf(key))
}

suspend fun NeovimClient.setCursor(
    bufferId: BufferId,
    pos: NeovimPosition,
) {
    execLuaNotify("buffer", "cursor", listOf(bufferId, pos.lnum, pos.col, pos.curswant))
}
