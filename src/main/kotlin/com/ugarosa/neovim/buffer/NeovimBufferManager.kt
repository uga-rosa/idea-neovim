package com.ugarosa.neovim.buffer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.createBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class NeovimBufferManager(
    private val scope: CoroutineScope,
) : Disposable {
    private val editorToHolder = ConcurrentHashMap<EditorEx, BufferHolder>()
    private val idToEditor = ConcurrentHashMap<BufferId, EditorEx>()

    fun listenEditorFactory() {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor as? EditorEx ?: return
                    scope.launch { register(editor) }
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    val editor = event.editor as? EditorEx ?: return
                    editorToHolder.remove(editor)?.dispose()
                    idToEditor.entries.removeIf { it.value == editor }
                }
            },
            this,
        )
    }

    private suspend fun register(editor: EditorEx): NeovimBuffer {
        val holder =
            editorToHolder.computeIfAbsent(editor) {
                BufferHolder(scope, editor)
            }
        holder.awaitId().also { id ->
            idToEditor.putIfAbsent(id, editor)
        }
        return holder.await()
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

    override fun dispose() {
        editorToHolder.values.forEach { holder ->
            holder.dispose()
        }
        editorToHolder.clear()
        idToEditor.clear()
    }

    companion object {
        suspend fun findById(id: BufferId): NeovimBuffer {
            return service<NeovimBufferManager>().findById(id)
        }

        suspend fun findByEditor(editor: EditorEx): NeovimBuffer {
            return service<NeovimBufferManager>().findByEditor(editor)
        }

        fun editor(id: BufferId): EditorEx {
            return service<NeovimBufferManager>().editor(id)
        }
    }
}

private class BufferHolder(
    private val scope: CoroutineScope,
    private val editor: EditorEx,
) : Disposable {
    private val client = service<NeovimClient>()
    private val deferredId = CompletableDeferred<BufferId>()
    private val deferredBuffer = CompletableDeferred<NeovimBuffer>()

    private val job =
        scope.launch {
            val bufferId = client.createBuffer()
            deferredId.complete(bufferId)
            val buffer = NeovimBuffer.create(scope, bufferId, editor)
            deferredBuffer.complete(buffer)
        }

    suspend fun awaitId(): BufferId = deferredId.await()

    suspend fun await(): NeovimBuffer = deferredBuffer.await()

    override fun dispose() {
        job.cancel()
    }
}
