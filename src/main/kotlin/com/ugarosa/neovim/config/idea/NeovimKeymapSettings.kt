package com.ugarosa.neovim.config.idea

interface NeovimKeymapSettings {
    fun getUserKeyMappings(): List<UserKeyMapping>

    fun setUserKeyMappings(mappings: List<UserKeyMapping>)
}
