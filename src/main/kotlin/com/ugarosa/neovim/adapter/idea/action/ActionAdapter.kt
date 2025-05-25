package com.ugarosa.neovim.adapter.idea.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.ugarosa.neovim.common.focusEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun executeAction(actionId: String) {
    val anAction = ActionManager.getInstance().getAction(actionId) ?: return
    val editor = focusEditor()

    withContext(Dispatchers.EDT) {
        ActionManager.getInstance().tryToExecute(
            anAction,
            null,
            editor?.component,
            "IdeaNeovim",
            true,
        ).waitFor(5_000)
    }
}
