package com.ugarosa.neovim.startup

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.ugarosa.neovim.buffer.NeovimBufferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class NeovimProjectListenerService(
    private val project: Project,
    private val scope: CoroutineScope,
) : Disposable {
    fun install() {
        registerEditorSelectionListener()
        registerFileWritableChangeListener()
    }

    private fun registerEditorSelectionListener() {
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val editor = (event.newEditor as? TextEditor)?.editor as? EditorEx ?: return
                    scope.launch {
                        val buffer = service<NeovimBufferManager>().findByEditor(editor)
                        buffer.onSelected()
                    }
                }
            },
        )
    }

    private fun registerFileWritableChangeListener() {
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events
                        .asSequence()
                        .filterIsInstance<VFilePropertyChangeEvent>()
                        .filter { it.propertyName == VirtualFile.PROP_WRITABLE }
                        .mapNotNull { FileDocumentManager.getInstance().getDocument(it.file) }
                        .flatMap { EditorFactory.getInstance().getEditors(it).toList() }
                        .filterIsInstance<EditorEx>()
                        .toList()
                        .forEach { editor ->
                            scope.launch {
                                val buffer = service<NeovimBufferManager>().findByEditor(editor)
                                buffer.changeModifiable()
                            }
                        }
                }
            },
        )
    }

    override fun dispose() {}
}
