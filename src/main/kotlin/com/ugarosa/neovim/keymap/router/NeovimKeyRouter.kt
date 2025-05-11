package com.ugarosa.neovim.keymap.router

import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation

interface NeovimKeyRouter {
    fun start()

    /**
     * Enqueues a key to be processed by the router.
     * Return true if the key was consumed (matched mapping or pending), false otherwise.
     */
    fun enqueueKey(
        key: NeovimKeyNotation,
        editor: Editor?,
    ): Boolean
}
