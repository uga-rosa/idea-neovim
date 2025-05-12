package com.ugarosa.neovim.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class NeovimActionManagerImpl() : NeovimActionManager {
    private val logger = myLogger()

    override suspend fun executeAction(
        actionId: String,
        editor: Editor?,
    ) {
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
                editor?.component,
                "IdeaNeovim",
                true,
            ).waitFor(5_000)
        }
    }
}
