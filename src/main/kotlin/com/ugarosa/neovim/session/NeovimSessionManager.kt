package com.ugarosa.neovim.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.createBuffer
import com.ugarosa.neovim.rpc.type.BufferId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class NeovimSessionManager {
    private val client = service<NeovimClient>()

    private data class Entry(
        val editor: Editor,
        val deferredSession: CompletableDeferred<NeovimEditorSession>,
        val bufferId: BufferId? = null,
    )

    private val byEditor = ConcurrentHashMap<Editor, Entry>()
    private val bufferIdToEditor = ConcurrentHashMap<BufferId, Editor>()

    suspend fun getSession(): NeovimEditorSession? {
        val editor = focusEditor() ?: return null
        return getSession(editor)
    }

    suspend fun getSession(editor: Editor): NeovimEditorSession {
        return byEditor[editor]?.deferredSession?.await()
            ?: error("Neovim session not found for editor: $editor")
    }

    suspend fun getSession(bufferId: BufferId): NeovimEditorSession {
        val editor = bufferIdToEditor[bufferId] ?: error("BufferId not found: $bufferId")
        return getSession(editor)
    }

    fun getEditor(bufferId: BufferId): Editor {
        return bufferIdToEditor[bufferId]
            ?: error("Editor not found for bufferId: $bufferId")
    }

    fun register(
        scope: CoroutineScope,
        editor: Editor,
        project: Project,
        disposable: Disposable,
    ) {
        val deferred = CompletableDeferred<NeovimEditorSession>()
        byEditor[editor] = Entry(editor, deferred)
        scope.launch {
            val bufferId = client.createBuffer()
            byEditor[editor] = Entry(editor, deferred, bufferId)
            bufferIdToEditor[bufferId] = editor
            val session = NeovimEditorSession.create(scope, editor, disposable, bufferId)
            deferred.complete(session)
        }
    }
}
