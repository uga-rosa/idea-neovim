package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.input(text: String) {
    notify("nvim_input", listOf(text))
}

suspend fun NvimClient.paste(text: String) {
    notify("nvim_paste", listOf(text, false, -1))
}

suspend fun NvimClient.sendDeletion(
    before: Int,
    after: Int,
) {
    execLuaNotify("buffer", "send_deletion", listOf(before, after))
}

suspend fun NvimClient.setCursor(
    bufferId: BufferId,
    pos: NvimPosition,
) {
    execLuaNotify("buffer", "cursor", listOf(bufferId, pos.line, pos.col, pos.curswant))
}
