package com.ugarosa.neovim.undo

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.ugarosa.neovim.common.unsafeFocusEditor
import org.intellij.lang.annotations.Language

class NeovimUndoAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        unsafeFocusEditor()?.project?.let { project ->
            val undoManager = project.service<NeovimUndoManager>()
            undoManager.undo()
        } ?: run {
            // Fallback to default undo action when you are in a non-project file
            val dataContext = e.dataContext
            e.getData(CommonDataKeys.EDITOR)?.let { editor ->
                EditorActionManager.getInstance()
                    .getActionHandler(IdeActions.ACTION_UNDO)
                    .execute(editor, null, dataContext)
            }
        }
    }

    companion object {
        @Language("devkit-action-id")
        const val ACTION_ID = "Neovim:UndoAction"
    }
}
