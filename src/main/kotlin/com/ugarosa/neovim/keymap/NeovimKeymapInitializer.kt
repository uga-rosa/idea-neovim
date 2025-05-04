package com.ugarosa.neovim.keymap

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import javax.swing.KeyStroke

// TODO: Get keymaps from user settings
fun initializeKeymap() {
    val keymapManager = KeymapManager.getInstance()
    val keymap = keymapManager.activeKeymap

    keys.forEach { key ->
        val shortcut = KeyboardShortcut(KeyStroke.getKeyStroke(key), null)
        keymap.addShortcut(NEOVIM_KEY_ACTION_ID, shortcut)
    }
}

private val keys =
    listOf(
        "ctrl R",
        "ESCAPE",
        "ENTER",
    )
