package com.ugarosa.neovim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY

class NeovimTypedActionHandler(
    private val originalHandler: TypedActionHandler,
) : TypedActionHandlerEx {
    override fun beforeExecute(
        p0: Editor,
        p1: Char,
        p2: DataContext,
        p3: ActionPlan,
    ) {
        // no-op
    }

    override fun execute(
        editor: Editor,
        char: Char,
        ctx: DataContext,
    ) {
        println("char = $char")
        println("ctx = $ctx")
        val session =
            editor.getUserData(NEOVIM_SESSION_KEY)
                ?: error("NeovimEditorSession does not exist")

        session.sendKeyAndSyncStatus(char.toString())
    }
}
