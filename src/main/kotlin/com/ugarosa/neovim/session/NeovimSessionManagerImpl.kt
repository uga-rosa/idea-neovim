package com.ugarosa.neovim.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.function.createBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class NeovimSessionManagerImpl : NeovimSessionManager {
    private val byEditor = ConcurrentHashMap<Editor, CompletableDeferred<NeovimEditorSession>>()
    private val byBufferId = ConcurrentHashMap<BufferId, CompletableDeferred<NeovimEditorSession>>()

    override suspend fun get(bufferId: BufferId): NeovimEditorSession {
        return byBufferId[bufferId]?.await()
            ?: error("Neovim session not found for bufferId: $bufferId")
    }

    override suspend fun get(editor: Editor): NeovimEditorSession {
        return byEditor[editor]?.await()
            ?: error("Neovim session not found for editor: $editor")
    }

    override fun register(
        scope: CoroutineScope,
        editor: Editor,
        project: Project,
        disposable: Disposable,
    ) {
        val deferred = CompletableDeferred<NeovimEditorSession>()
        byEditor[editor] = deferred
        scope.launch {
            val bufferId = createBuffer(getClient()) ?: error("Failed to create buffer")
            byBufferId[bufferId] = deferred
            val session = NeovimEditorSession.create(scope, editor, project, disposable, bufferId)
            deferred.complete(session)
        }
    }
}
