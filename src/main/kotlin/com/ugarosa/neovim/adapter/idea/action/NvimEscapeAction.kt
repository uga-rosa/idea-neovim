package com.ugarosa.neovim.adapter.idea.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.ugarosa.neovim.bus.EscapeInsert
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.common.unsafeFocusEditor
import org.intellij.lang.annotations.Language

class NvimEscapeAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val editor = unsafeFocusEditor() ?: return
        val event = EscapeInsert(editor)
        service<IdeaToNvimBus>().tryEmit(event)
    }

    companion object {
        @Language("devkit-action-id")
        const val ACTION_ID = "NvimEscapeAction"
    }
}
