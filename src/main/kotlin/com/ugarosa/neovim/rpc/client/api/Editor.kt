package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimPosition

suspend fun NeovimClient.input(text: String) {
    notify("nvim_input", listOf(text))
}

suspend fun NeovimClient.paste(text: String) {
    notify("nvim_paste", listOf(text, false, -1))
}

suspend fun NeovimClient.sendDeletion(
    before: Int,
    after: Int,
) {
    execLuaNotify("buffer", "send_deletion", listOf(before, after))
}

suspend fun NeovimClient.setCursor(
    bufferId: BufferId,
    pos: NeovimPosition,
) {
    execLuaNotify("buffer", "cursor", listOf(bufferId, pos.line, pos.col, pos.curswant))
}
