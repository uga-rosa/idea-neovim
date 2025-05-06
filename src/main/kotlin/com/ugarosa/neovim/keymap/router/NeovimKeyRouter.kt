package com.ugarosa.neovim.keymap.router

import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.config.idea.UserKeyMapping
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation

interface NeovimKeyRouter {
    fun start()

    fun enqueueKey(
        key: NeovimKeyNotation,
        editor: Editor,
    )

    fun setUserMappings(mappings: List<UserKeyMapping>)
}
