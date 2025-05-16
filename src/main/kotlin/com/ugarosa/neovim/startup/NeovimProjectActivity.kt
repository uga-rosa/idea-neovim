package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.session.NeovimSessionManager
import com.ugarosa.neovim.window.NeovimWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimProjectActivity(
    private val scope: CoroutineScope,
) : ProjectActivity {
    private val sessionManager = service<NeovimSessionManager>()

    override suspend fun execute(project: Project) {
        val disposable = project.service<ProjectDisposable>()

        setupEditorFactoryListener(disposable)
        initializeExistingEditors(disposable)
        setupBufferActivationOnEditorSwitch(project, disposable)
        setupWritablePropertyChangeListener(project, disposable)
    }

    private fun setupEditorFactoryListener(disposable: Disposable) {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    sessionManager.register(scope, event.editor, disposable)
                }
            },
            disposable,
        )
    }

    private fun initializeExistingEditors(disposable: Disposable) {
        EditorFactory.getInstance().allEditors.forEach { editor ->
            sessionManager.register(scope, editor, disposable)
        }
    }

    private suspend fun setupBufferActivationOnEditorSwitch(
        project: Project,
        disposable: Disposable,
    ) {
        // Activate the buffer in the currently focused editor
        focusEditor()?.let {
            sessionManager.getSession(it).activateBuffer()
        }

        // Activate the buffer when the editor selection changes
        project.messageBus.connect(disposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val editor = event.manager.selectedTextEditor ?: return
                    scope.launch {
                        sessionManager.getSession(editor).activateBuffer()
                        editor.project?.service<NeovimWindowManager>()?.walkSplitLayout()
                    }
                }
            },
        )
    }

    private fun setupWritablePropertyChangeListener(
        project: Project,
        disposable: Disposable,
    ) {
        project.messageBus.connect(disposable).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events
                        .filterIsInstance<VFilePropertyChangeEvent>()
                        .filter { it.propertyName == VirtualFile.PROP_WRITABLE }
                        .mapNotNull { FileDocumentManager.getInstance().getDocument(it.file) }
                        .flatMap { EditorFactory.getInstance().getEditors(it).toList() }
                        .forEach { editor ->
                            scope.launch {
                                sessionManager.getSession(editor).changeModifiable()
                            }
                        }
                }
            },
        )
    }
}
