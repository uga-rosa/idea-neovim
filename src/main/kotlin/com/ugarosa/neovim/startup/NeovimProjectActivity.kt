package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.keymap.NeovimTypedActionHandler
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import com.ugarosa.neovim.session.NeovimEditorSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimProjectActivity(
    private val scope: CoroutineScope,
) : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = project.service<PluginDisposable>()
        installNeovimTypedActionHandler()
        initializeEditorSessions(project, disposable)
        setupBufferActivationOnEditorSwitch(project, disposable)
    }

    private fun installNeovimTypedActionHandler() {
        val typedAction = TypedAction.getInstance()
        val originalHandler = typedAction.handler
        typedAction.setupRawHandler(NeovimTypedActionHandler(originalHandler))
    }

    private suspend fun initializeEditorSessions(
        project: Project,
        disposable: Disposable,
    ) {
        val client = ApplicationManager.getApplication().service<NeovimRpcClient>()
        // Initialize all existing editors
        EditorFactory.getInstance().allEditors.forEach { editor ->
            NeovimEditorSession.create(client, scope, editor, project)
        }
        // Initialize when a new editor is created
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    scope.launch {
                        NeovimEditorSession.create(client, scope, event.editor, project)
                    }
                }
            },
            disposable,
        )
    }

    private fun setupBufferActivationOnEditorSwitch(
        project: Project,
        disposable: Disposable,
    ) {
        // Activate the buffer in the currently selected editor
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedTextEditor
        selectedEditor?.getUserData(NEOVIM_SESSION_KEY)
            ?.activateBuffer()
        // Activate the buffer when the editor selection changes
        project.messageBus.connect(disposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val newEditor = event.newEditor
                    if (newEditor is TextEditor) {
                        newEditor.editor.getUserData(NEOVIM_SESSION_KEY)
                            ?.activateBuffer()
                    }
                    super.selectionChanged(event)
                }
            },
        )
    }
}
