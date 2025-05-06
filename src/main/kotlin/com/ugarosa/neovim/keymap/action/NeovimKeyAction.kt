package com.ugarosa.neovim.keymap.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import java.awt.event.KeyEvent

// Set id in plugin.xml
const val NEOVIM_KEY_ACTION_ID = "NeovimKeyAction"

class NeovimKeyAction : AnAction() {
    private val logger = thisLogger()

    override fun actionPerformed(e: AnActionEvent) {
        val inputEvent = e.inputEvent as? KeyEvent ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val session = editor.getUserData(NEOVIM_SESSION_KEY) ?: return

        val keyNotation =
            NeovimKeyNotation.fromKeyEvent(inputEvent)
                ?: return logger.warn("Key event not supported: $inputEvent")

        logger.trace("pressed key: $keyNotation")
        session.sendKeyAndSyncStatus(keyNotation.toString())
        inputEvent.consume()
    }

    override fun toString(): String {
        return "NeovimKeyAction (Hijack key event)"
    }
}
