package com.ugarosa.neovim.mode

interface NeovimModeManager {
    fun getMode(): NeovimMode

    fun setMode(newMode: NeovimMode): Boolean
}
