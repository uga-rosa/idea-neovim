package com.ugarosa.neovim.session

import arrow.core.getOrElse
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.rpc.BufLinesEvent
import com.ugarosa.neovim.rpc.NeovimMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.createBuffer
import com.ugarosa.neovim.rpc.getMode
import com.ugarosa.neovim.rpc.input
import com.ugarosa.neovim.rpc.maybeBufLinesEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

val NEOVIM_SESSION_KEY = Key.create<NeovimEditorSession>("NEOVIM_SESSION_KEY")

class NeovimEditorSession private constructor(
    private val client: NeovimClient,
    private val scope: CoroutineScope,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
) {
    private val logger = thisLogger()
    private var currentMode = NeovimMode.NORMAL

    companion object {
        suspend fun create(
            client: NeovimClient,
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
        ): NeovimEditorSession? {
            return scope.async(Dispatchers.IO) {
                val bufferId =
                    createBuffer(client).getOrElse {
                        thisLogger().error("Failed to create buffer: $it")
                        return@async null
                    }
                val documentHandler = NeovimDocumentHandler.create(client, editor, bufferId)
                val cursorHandler = NeovimCursorHandler(client, editor)
                val statusLineHandler = StatusLineHandler(project)
                val session =
                    NeovimEditorSession(
                        client,
                        scope,
                        documentHandler,
                        cursorHandler,
                        statusLineHandler,
                    )

                client.registerPushHandler { push ->
                    val event = maybeBufLinesEvent(push)
                    if (event?.bufferId == bufferId) {
                        session.handleBufferLinesEvent(event)
                    }
                }

                editor.putUserData(NEOVIM_SESSION_KEY, session)

                session
            }.await()
        }
    }

    fun activateBuffer() {
        scope.launch {
            documentHandler.activateBuffer()
        }
    }

    fun sendKeyAndSyncStatus(key: String) {
        scope.launch {
            input(client, key)
            cursorHandler.syncCursorFromNeovimToIdea()
            syncNeovimMode()
        }
    }

    fun syncCursorFromIdeaToNeovim() {
        scope.launch {
            cursorHandler.syncCursorFromIdeaToNeovim()
        }
    }

    private suspend fun handleBufferLinesEvent(e: BufLinesEvent) {
        documentHandler.applyBufferLinesEvent(e)
        cursorHandler.syncCursorFromNeovimToIdea()
    }

    private suspend fun syncNeovimMode() {
        currentMode =
            getMode(client).getOrElse {
                logger.warn("Failed to get mode: $it")
                return
            }
        cursorHandler.changeCursorShape(currentMode)
        statusLineHandler.updateStatusLine(currentMode)
    }
}
