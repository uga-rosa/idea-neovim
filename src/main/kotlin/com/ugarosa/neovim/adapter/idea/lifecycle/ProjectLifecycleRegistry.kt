package com.ugarosa.neovim.adapter.idea.lifecycle

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.ugarosa.neovim.bus.ChangeModifiable
import com.ugarosa.neovim.bus.EditorSelected
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.logger.myLogger

@Service(Service.Level.PROJECT)
class ProjectLifecycleRegistry(
    private val project: Project,
) : Disposable {
    private val logger = myLogger()
    private val bus = service<IdeaToNvimBus>()

    init {
        emitInitialEditorSelection()
        subscribeToEditorSelectionChanges()
        subscribeToFileWritablePropertyChanges()
    }

    private fun emitInitialEditorSelection() {
        val manager = FileEditorManager.getInstance(project)
        val editor = manager.selectedTextEditor as? EditorEx ?: return
        logger.info("Initial editor selected: $editor")
        bus.tryEmit(EditorSelected(editor))
    }

    private fun subscribeToEditorSelectionChanges() {
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val editor = (event.newEditor as? TextEditor)?.editor as? EditorEx ?: return
                    logger.info("Editor selected: $editor")
                    bus.tryEmit(EditorSelected(editor))
                }
            },
        )
    }

    private fun subscribeToFileWritablePropertyChanges() {
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
                        .forEach { editor ->
                            logger.info("File writable property changed: $editor")
                            bus.tryEmit(ChangeModifiable(editor))
                        }
                }
            },
        )
    }

    override fun dispose() {}
}
