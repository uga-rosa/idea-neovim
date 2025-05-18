package com.ugarosa.neovim.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.ugarosa.neovim.window.NeovimWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener

class NeovimProjectActivity(
    private val scope: CoroutineScope,
) : ProjectActivity {
    override suspend fun execute(project: Project) {
        registerFileEditorManagerListener(project)
        registerSplitterContainerListener(project)

        // Initial layout sync
        project.service<NeovimWindowManager>().syncLayout()
    }

    private fun onLayoutChanged(project: Project) {
        scope.launch {
            project.service<NeovimWindowManager>().syncLayout()
        }
    }

    private fun registerFileEditorManagerListener(project: Project) {
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(
                    source: FileEditorManager,
                    file: VirtualFile,
                ) {
                    onLayoutChanged(project)
                }

                override fun fileClosed(
                    source: FileEditorManager,
                    file: VirtualFile,
                ) {
                    onLayoutChanged(project)
                }

                override fun selectionChanged(event: FileEditorManagerEvent) {
                    onLayoutChanged(project)
                }
            },
        )
    }

    private fun registerSplitterContainerListener(project: Project) {
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val splitters = manager.splitters
        splitters.addContainerListener(
            object : ContainerListener {
                override fun componentAdded(e: ContainerEvent?) {
                    onLayoutChanged(project)
                }

                override fun componentRemoved(e: ContainerEvent?) {
                    onLayoutChanged(project)
                }
            },
        )
    }
}
