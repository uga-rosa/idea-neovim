package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.session.NeovimEditorSession
import com.ugarosa.neovim.session.getSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimProjectActivity(
    private val scope: CoroutineScope,
) : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = project.service<ProjectDisposable>()

        setupEditorFactoryListener(project, disposable)
        initializeExistingEditors(project, disposable)
        setupBufferActivationOnEditorSwitch(project, disposable)
    }

    private fun setupEditorFactoryListener(
        project: Project,
        disposable: Disposable,
    ) {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    initializeEditor(event.editor, project, disposable)
                }
            },
            disposable,
        )
    }

    private fun initializeExistingEditors(
        project: Project,
        disposable: Disposable,
    ) {
        EditorFactory.getInstance().allEditors.forEach { editor ->
            initializeEditor(editor, project, disposable)
        }
    }

    private fun initializeEditor(
        editor: Editor,
        project: Project,
        disposable: Disposable,
    ) {
        NeovimEditorSession.create(scope, editor, project, disposable)
    }

    private suspend fun setupBufferActivationOnEditorSwitch(
        project: Project,
        disposable: Disposable,
    ) {
        // Activate the buffer in the currently selected editor
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedTextEditor
        selectedEditor?.getSession()?.activateBuffer()
        // Activate the buffer when the editor selection changes
        project.messageBus.connect(disposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val newEditor = event.newEditor
                    if (newEditor is TextEditor) {
                        scope.launch {
                            newEditor.editor.getSession().activateBuffer()
                        }
                    }
                    super.selectionChanged(event)
                }
            },
        )
    }
}
