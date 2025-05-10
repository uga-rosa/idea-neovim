package com.ugarosa.neovim.mode

interface NeovimModeManager {
    fun get(): NeovimMode

    fun set(newMode: NeovimMode): Boolean
}
