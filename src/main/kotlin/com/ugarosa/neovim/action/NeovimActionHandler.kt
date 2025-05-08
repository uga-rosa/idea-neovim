package com.ugarosa.neovim.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimActionHandler(
    private val editor: Editor,
) {
    private val logger = thisLogger()

    suspend fun executeAction(actionId: String) {
        val anAction =
            ActionManager.getInstance().getAction(actionId)
                ?: run {
                    logger.warn("Action not found: $actionId")
                    return
                }
        withContext(Dispatchers.EDT) {
            ActionManager.getInstance().tryToExecute(
                anAction,
                null,
                editor.contentComponent,
                "IdeaNeovim",
                true,
            ).waitFor(5_000)
        }
    }
}
