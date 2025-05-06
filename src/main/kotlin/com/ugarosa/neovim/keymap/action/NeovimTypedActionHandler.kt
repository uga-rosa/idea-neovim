package com.ugarosa.neovim.keymap.action

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY

class NeovimTypedActionHandler(
    private val project: Project,
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
                ?: throw IllegalStateException("NeovimEditorSession does not exist")

        if (session.getMode().kind == NeovimModeKind.INSERT) {
            logger.trace("Typed action in insert mode: $char")
            WriteCommandAction.runWriteCommandAction(project) {
                delegate.execute(editor, char, ctx)
            }
        } else {
            logger.trace("Send char to Neovim: $char")
            session.sendKeyAndSyncStatus(char.toString())
        }
    }
}
