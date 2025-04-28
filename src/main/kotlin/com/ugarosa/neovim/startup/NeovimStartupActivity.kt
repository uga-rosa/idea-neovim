package com.ugarosa.neovim.startup

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.ugarosa.neovim.infra.NeovimProcessManager
import com.ugarosa.neovim.infra.NeovimRpcClient

class NeovimStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val processManager = NeovimProcessManager()
        val client =
            NeovimRpcClient(
                input = processManager.getInputStream(),
                output = processManager.getOutputStream(),
            )
        inject(client)

        EditorFactory.getInstance().allEditors.forEach { editor ->
            client.initializeNeovimBuffer(editor)
        }
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    client.initializeNeovimBuffer(event.editor)
                }
            },
            project,
        )
    }
}
