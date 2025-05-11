package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.domain.NeovimPosition
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun input(
    client: NeovimRpcClient,
    key: String,
) {
    client.notify("nvim_input", listOf(key))
}

suspend fun setCursor(
    client: NeovimRpcClient,
    bufferId: BufferId,
    pos: NeovimPosition,
) {
    execLuaNotify(
        client,
        "buffer",
        "cursor",
        listOf(bufferId, pos.lnum, pos.col + 1, pos.curswant),
    )
}
