package com.ugarosa.neovim.adapter.idea.lifecycle

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.adapter.coordinator.BufferCoordinator
import com.ugarosa.neovim.adapter.idea.ui.isCustomEditor
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.createBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class IdeaBufferCoordinatorRegistry(
    private val scope: CoroutineScope,
) : Disposable {
    private val client = service<NvimClient>()
    private val bufferMap = ConcurrentHashMap<EditorEx, BufferCoordinator>()

    init {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor as? EditorEx ?: return
                    if (isCustomEditor(editor)) return
                    scope.launch { register(editor) }
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    val editor = event.editor as? EditorEx ?: return
                    bufferMap.remove(editor)?.dispose()
                }
            },
            this,
        )
    }

    private suspend fun register(editor: EditorEx) {
        val bufferId = client.createBuffer()
        val buffer = BufferCoordinator.getInstance(scope, bufferId, editor)
        bufferMap[editor] = buffer
    }

    override fun dispose() {
        bufferMap.values.forEach { it.dispose() }
        bufferMap.clear()
    }
}
