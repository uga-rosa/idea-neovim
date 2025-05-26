package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.input(text: String) {
    notify("nvim_input", listOf(text))
}

suspend fun NvimClient.insert(
    beforeDelete: Int,
    afterDelete: Int,
    inputBefore: String,
    inputAfter: String,
): Int =
    execLua("insert", "input", listOf(beforeDelete, afterDelete, inputBefore, inputAfter))
        .asInt()

suspend fun NvimClient.setCursor(
    bufferId: BufferId,
    pos: NvimPosition,
) {
    execLuaNotify("buffer", "cursor", listOf(bufferId, pos.line, pos.col, pos.curswant))
}
