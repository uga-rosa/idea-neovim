package com.ugarosa.neovim.config

import com.intellij.openapi.options.Configurable
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class NeovimKeymapConfigurable : Configurable {
    private val panel = JPanel(BorderLayout())

    override fun getDisplayName(): String {
        return "Neovim Keymap"
    }

    override fun createComponent(): JComponent {
        // TODO
        return panel
    }

    override fun isModified(): Boolean {
        // TODO
        return false
    }

    override fun apply() {
        // TODO
    }
}
