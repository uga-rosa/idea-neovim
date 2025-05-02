package com.ugarosa.neovim.keymap

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.neovimNotation
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

        val key = neovimNotation(inputEvent)
        logger.debug("pressed key: $key")
        session.sendKeyAndSyncStatus(key)
    }

    override fun toString(): String {
        return "NeovimKeyAction (Hijack key event)"
    }
}
