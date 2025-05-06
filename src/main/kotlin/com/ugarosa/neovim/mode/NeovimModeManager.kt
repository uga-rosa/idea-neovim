package com.ugarosa.neovim.mode

import com.ugarosa.neovim.rpc.event.NeovimMode

interface NeovimModeManager {
    fun getMode(): NeovimMode

    fun setMode(newMode: NeovimMode): Boolean
}
