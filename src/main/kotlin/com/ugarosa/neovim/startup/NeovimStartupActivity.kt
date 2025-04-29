package com.ugarosa.neovim.startup

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.infra.NeovimProcessManager
import com.ugarosa.neovim.infra.NeovimRpcClient
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import com.ugarosa.neovim.session.NeovimEditorSession

class NeovimStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        inject()

        val processManager = NeovimProcessManager()
        val client =
            NeovimRpcClient(
                input = processManager.getInputStream(),
                output = processManager.getOutputStream(),
            )

        EditorFactory.getInstance().allEditors.forEach { editor ->
            NeovimEditorSession(client, editor)
        }
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    NeovimEditorSession(client, event.editor)
                }
            },
            project,
        )

        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedTextEditor
        if (selectedEditor != null) {
            val session = selectedEditor.getUserData(NEOVIM_SESSION_KEY)
            session?.activateBuffer()
        }

        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val newEditor = event.newEditor
                    if (newEditor is TextEditor) {
                        val editor = newEditor.editor
                        val session = editor.getUserData(NEOVIM_SESSION_KEY)
                        session?.activateBuffer()
                    }
                    super.selectionChanged(event)
                }
            },
        )
    }
}
