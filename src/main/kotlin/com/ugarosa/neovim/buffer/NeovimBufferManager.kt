package com.ugarosa.neovim.buffer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.createBuffer
import com.ugarosa.neovim.window.WindowId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class NeovimBufferManager(
    private val scope: CoroutineScope,
) {
    private val editorToHolder = ConcurrentHashMap<EditorEx, BufferHolder>()
    private val idToEditor = ConcurrentHashMap<BufferId, EditorEx>()

    suspend fun register(
        editor: EditorEx,
        windowId: WindowId,
    ): NeovimBuffer {
        editorToHolder[editor]?.let { holder ->
            return holder.await().apply {
                setWindow(windowId)
            }
        }

        val holder = BufferHolder(scope, editor)
        editorToHolder[editor] = holder
        val bufferId = holder.awaitId()
        idToEditor[bufferId] = editor
        return holder.await().apply {
            setWindow(windowId)
        }
    }

    suspend fun findById(id: BufferId): NeovimBuffer {
        val editor = idToEditor[id] ?: error("Buffer $id not registered")
        val holder = editorToHolder[editor] ?: error("Editor not registered")
        return holder.await()
    }

    suspend fun findByEditor(editor: EditorEx): NeovimBuffer {
        val holder = editorToHolder[editor] ?: error("Editor not registered")
        return holder.await()
    }

    fun editor(id: BufferId): EditorEx {
        return idToEditor[id] ?: error("Buffer $id not registered")
    }

    companion object {
        suspend fun findById(id: BufferId): NeovimBuffer {
            val project = focusProject() ?: error("No project focused")
            return project.service<NeovimBufferManager>().findById(id)
        }

        suspend fun findByEditor(editor: EditorEx): NeovimBuffer {
            val project = focusProject() ?: error("No project focused")
            return project.service<NeovimBufferManager>().findByEditor(editor)
        }

        suspend fun editor(id: BufferId): EditorEx {
            val project = focusProject() ?: error("No project focused")
            return project.service<NeovimBufferManager>().editor(id)
        }
    }
}

private class BufferHolder(
    private val scope: CoroutineScope,
    private val editor: EditorEx,
) {
    private val client = service<NeovimClient>()
    private val deferredId = CompletableDeferred<BufferId>()
    private val deferredBuffer = CompletableDeferred<NeovimBuffer>()

    init {
        scope.launch {
            val bufferId = client.createBuffer()
            deferredId.complete(bufferId)
            val buffer = NeovimBuffer.create(scope, bufferId, editor)
            deferredBuffer.complete(buffer)
        }
    }

    suspend fun awaitId(): BufferId = deferredId.await()

    suspend fun await(): NeovimBuffer = deferredBuffer.await()
}
