package com.ugarosa.neovim.session

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.function.createBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class NeovimSessionManagerImpl : NeovimSessionManager {
    private val client = getClient()

    private data class Entry(
        val editor: Editor,
        val deferredSession: CompletableDeferred<NeovimEditorSession>,
        val bufferId: BufferId? = null,
    )

    private val byEditor = ConcurrentHashMap<Editor, Entry>()
    private val bufferIdToEditor = ConcurrentHashMap<BufferId, Editor>()

    override suspend fun getSession(): NeovimEditorSession? {
        val editor =
            withContext(Dispatchers.EDT) {
                val focusOwner = IdeFocusManager.getGlobalInstance().focusOwner
                val dataContext = DataManager.getInstance().getDataContext(focusOwner)
                CommonDataKeys.EDITOR.getData(dataContext)
            } ?: return null
        return getSession(editor)
    }

    override suspend fun getSession(editor: Editor): NeovimEditorSession {
        return byEditor[editor]?.deferredSession?.await()
            ?: error("Neovim session not found for editor: $editor")
    }

    override suspend fun getSession(bufferId: BufferId): NeovimEditorSession {
        val bufferId = bufferIdToEditor[bufferId] ?: error("BufferId not found: $bufferId")
        return getSession(bufferId)
    }

    override suspend fun getEditor(bufferId: BufferId): Editor {
        return bufferIdToEditor[bufferId]
            ?: error("Editor not found for bufferId: $bufferId")
    }

    override fun register(
        scope: CoroutineScope,
        editor: Editor,
        project: Project,
        disposable: Disposable,
    ) {
        val deferred = CompletableDeferred<NeovimEditorSession>()
        byEditor[editor] = Entry(editor, deferred)
        scope.launch {
            val bufferId = createBuffer(client) ?: error("Failed to create buffer")
            byEditor[editor] = Entry(editor, deferred, bufferId)
            bufferIdToEditor[bufferId] = editor
            val session = NeovimEditorSession.create(scope, editor, disposable, bufferId)
            deferred.complete(session)
        }
    }
}
