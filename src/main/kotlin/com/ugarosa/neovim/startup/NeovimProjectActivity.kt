package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
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
import com.ugarosa.neovim.common.CARET_LISTENER_GUARD_KEY
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.cursor.NeovimCaretListener
import com.ugarosa.neovim.keymap.NeovimTypedActionHandler
import com.ugarosa.neovim.rpc.client.NeovimRpcClientImpl
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import com.ugarosa.neovim.session.NeovimEditorSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimProjectActivity(
    private val scope: CoroutineScope,
) : ProjectActivity {
    override suspend fun execute(project: Project) {
        val client = ApplicationManager.getApplication().service<NeovimRpcClientImpl>()
        val disposable = project.service<PluginDisposable>()

        installNeovimTypedActionHandler()
        setupEditorFactoryListener(project, client, disposable)
        initializeExistingEditors(project, client, disposable)
        setupBufferActivationOnEditorSwitch(project, disposable)
    }

    private fun installNeovimTypedActionHandler() {
        val typedAction = TypedAction.getInstance()
        val originalHandler = typedAction.handler
        typedAction.setupRawHandler(NeovimTypedActionHandler(originalHandler))
    }

    private fun setupEditorFactoryListener(
        project: Project,
        client: NeovimRpcClientImpl,
        disposable: Disposable,
    ) {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    scope.launch {
                        initializeEditor(event.editor, project, client, disposable)
                    }
                }
            },
            disposable,
        )
    }

    private suspend fun initializeExistingEditors(
        project: Project,
        client: NeovimRpcClientImpl,
        disposable: Disposable,
    ) {
        EditorFactory.getInstance().allEditors.forEach { editor ->
            initializeEditor(editor, project, client, disposable)
        }
    }

    private suspend fun initializeEditor(
        editor: Editor,
        project: Project,
        client: NeovimRpcClientImpl,
        disposable: Disposable,
    ) {
        ListenerGuard(
            NeovimCaretListener(editor),
            { editor.caretModel.addCaretListener(it, disposable) },
            { editor.caretModel.removeCaretListener(it) },
        ).let { editor.putUserData(CARET_LISTENER_GUARD_KEY, it) }
        NeovimEditorSession.create(client, scope, editor, project)
            ?.let { editor.putUserData(NEOVIM_SESSION_KEY, it) }
            ?: throw IllegalStateException("Failed to create Neovim session")
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
