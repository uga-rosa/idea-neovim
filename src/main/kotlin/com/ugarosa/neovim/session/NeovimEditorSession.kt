package com.ugarosa.neovim.session

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.rpc.BufLinesEvent
import com.ugarosa.neovim.rpc.NeovimFunctions
import com.ugarosa.neovim.rpc.NeovimMode
import com.ugarosa.neovim.rpc.NeovimRpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

val NEOVIM_SESSION_KEY = Key.create<NeovimEditorSession>("NEOVIM_SESSION_KEY")

class NeovimEditorSession private constructor(
    private val rpcClient: NeovimRpcClient,
    private val editor: Editor,
    private val scope: CoroutineScope,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
) {
    private var currentMode = NeovimMode.NORMAL

    companion object {
        suspend fun create(
            rpcClient: NeovimRpcClient,
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
        ): NeovimEditorSession {
            return scope.async(Dispatchers.IO) {
                val bufferId = NeovimFunctions.createBuffer(rpcClient)
                val documentHandler = NeovimDocumentHandler.create(rpcClient, editor, bufferId)
                val cursorHandler = NeovimCursorHandler(rpcClient, editor)
                val statusLineHandler = StatusLineHandler(project)
                val session =
                    NeovimEditorSession(
                        rpcClient,
                        editor,
                        scope,
                        documentHandler,
                        cursorHandler,
                        statusLineHandler,
                    )

                rpcClient.registerPushHandler { push ->
                    val event = NeovimFunctions.maybeBufLinesEvent(push)
                    if (event?.bufferId == bufferId) {
                        session.handleBufferLinesEvent(event)
                    }
                }

                session
            }.await()
        }
    }

    fun setToEditor() {
        editor.putUserData(NEOVIM_SESSION_KEY, this)
    }

    fun activateBuffer() {
        scope.launch {
            documentHandler.activateBuffer()
        }
    }

    fun sendKeyAndSyncStatus(key: String) {
        scope.launch {
            NeovimFunctions.input(rpcClient, key)
            cursorHandler.syncCursorFromNeovimToIdea()
            syncNeovimMode()
        }
    }

    private suspend fun handleBufferLinesEvent(e: BufLinesEvent) {
        documentHandler.applyBufferLinesEvent(e)
        cursorHandler.syncCursorFromNeovimToIdea()
    }

    private suspend fun syncNeovimMode() {
        currentMode = NeovimFunctions.getMode(rpcClient)
        cursorHandler.changeCursorShape(currentMode)
        statusLineHandler.updateStatusLine(currentMode)
    }
}
