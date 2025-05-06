package com.ugarosa.neovim.config.idea

import com.intellij.openapi.options.Configurable
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

data class UserKeyMapping(
    val modes: List<NeovimModeKind>,
    val lhs: List<NeovimKeyNotation>,
    val rhs: List<KeyMappingAction>,
)

sealed class KeyMappingAction {
    data class SendToNeovim(val key: NeovimKeyNotation) : KeyMappingAction()

    data class ExecuteIdeaAction(val actionId: String) : KeyMappingAction()
}

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
