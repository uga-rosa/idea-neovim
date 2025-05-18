package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.window.WindowId

suspend fun NeovimClient.resetWindow(): WindowId =
    execLua("window", "reset", emptyList())
        .asWindowId()

enum class SplitDirection(val value: String) {
    Left("left"),
    Right("right"),
    Above("above"),
    Below("below"),
}

suspend fun NeovimClient.splitWindow(
    windowId: WindowId,
    direction: SplitDirection,
): WindowId =
    request(
        "nvim_open_win",
        listOf(
            0,
            false,
            mapOf(
                "win" to windowId,
                "split" to direction.value,
            ),
        ),
    ).asWindowId()

suspend fun NeovimClient.winSetBuf(
    windowId: WindowId,
    bufferId: BufferId,
) {
    notify(
        "nvim_win_set_buf",
        listOf(windowId, bufferId),
    )
}
