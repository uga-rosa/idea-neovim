package com.ugarosa.neovim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx
import com.ugarosa.neovim.infra.NeovimRpcClient

class NeovimTypedActionHandler(
    private val originalHandler: TypedActionHandler,
    private val client: NeovimRpcClient,
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
        if (char in setOf('h', 'j', 'k', 'l')) {
            client.sendInput(char.toString())
            val pos = client.getCursor(editor)
            editor.caretModel.moveToLogicalPosition(pos)
        } else {
            originalHandler.execute(editor, char, ctx)
        }
    }
}
