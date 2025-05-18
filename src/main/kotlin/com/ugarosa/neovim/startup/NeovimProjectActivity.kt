package com.ugarosa.neovim.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.buffer.NeovimBufferManager

class NeovimProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<NeovimProjectListenerService>().install()
        fireInitialSelection(project)
    }

    private suspend fun fireInitialSelection(project: Project) {
        val manager = FileEditorManager.getInstance(project)
        val editor = manager.selectedTextEditor as? EditorEx ?: return
        val buffer = service<NeovimBufferManager>().findByEditor(editor)
        buffer.onSelected()
    }
}
