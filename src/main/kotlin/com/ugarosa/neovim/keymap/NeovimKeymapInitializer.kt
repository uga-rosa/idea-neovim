package com.ugarosa.neovim.keymap

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import javax.swing.KeyStroke

class NeovimKeymapInitializer : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        super.appFrameCreated(commandLineArgs)

        // Initialize the keymap for Neovim
        val keymapManager = KeymapManager.getInstance()
        val keymap = keymapManager.activeKeymap

        val shortcut = KeyboardShortcut(KeyStroke.getKeyStroke("ctrl R"), null)
        keymap.addShortcut(NEOVIM_KEY_ACTION_ID, shortcut)
    }
}
