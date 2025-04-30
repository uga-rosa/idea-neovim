package com.ugarosa.neovim.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.infra.NeovimRpcClient
import com.ugarosa.neovim.service.CoroutineService
import com.ugarosa.neovim.service.PluginDisposable
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import com.ugarosa.neovim.session.NeovimEditorSession

class NeovimStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        inject()

        val client = ApplicationManager.getApplication().service<NeovimRpcClient>()
        val scope = project.service<CoroutineService>().scope

        // Initialize all existing editors
        EditorFactory.getInstance().allEditors.forEach { editor ->
            val session = NeovimEditorSession(client, editor, scope)
            editor.putUserData(NEOVIM_SESSION_KEY, session)
        }
        // Initialize new editors
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val session = NeovimEditorSession(client, event.editor, scope)
                    event.editor.putUserData(NEOVIM_SESSION_KEY, session)
                }
            },
            project.service<PluginDisposable>(),
        )

        // Activate the buffer in the currently selected editor
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedTextEditor
        if (selectedEditor != null) {
            val session = selectedEditor.getUserData(NEOVIM_SESSION_KEY)
            session?.activateBuffer()
        }
        // Activate the buffer when the editor selection changes
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val newEditor = event.newEditor
                    if (newEditor is TextEditor) {
                        val session = newEditor.editor.getUserData(NEOVIM_SESSION_KEY)
                        session?.activateBuffer()
                    }
                    super.selectionChanged(event)
                }
            },
        )
    }
}
