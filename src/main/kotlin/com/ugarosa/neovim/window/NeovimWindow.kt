package com.ugarosa.neovim.window

import com.ugarosa.neovim.buffer.NeovimBuffer

sealed interface NeovimWindow {
    data class Grid(
        val windowId: WindowId,
        val buffer: NeovimBuffer,
    ) : NeovimWindow

    data class Diff(
        val left: Grid,
        val right: Grid,
    ) : NeovimWindow

    data class Patch(
        val left: Grid,
        val mid: Grid,
        val right: Grid,
    ) : NeovimWindow
}
