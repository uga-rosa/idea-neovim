package com.ugarosa.neovim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY

class NeovimTypedActionHandler(
    private val delegate: TypedActionHandler,
) : TypedActionHandler {
    private val logger = thisLogger()

    override fun execute(
        editor: Editor,
        char: Char,
        ctx: DataContext,
    ) {
        val session =
            editor.getUserData(NEOVIM_SESSION_KEY)
                ?: error("NeovimEditorSession does not exist")
        logger.debug("TypedActionHandler: $char")
        session.sendKeyAndSyncStatus(char.toString())
    }
}
