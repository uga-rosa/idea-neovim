package com.ugarosa.neovim.session

import arrow.core.getOrElse
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.BufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.function.NeovimMode
import com.ugarosa.neovim.rpc.function.createBuffer
import com.ugarosa.neovim.rpc.function.getMode
import com.ugarosa.neovim.rpc.function.input
import com.ugarosa.neovim.statusline.StatusLineHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val NEOVIM_SESSION_KEY = Key.create<NeovimEditorSession>("NEOVIM_SESSION_KEY")

/**
 * Represents a session of Neovim editor.
 * Manages the interaction between Neovim and the IntelliJ editor.
 * Actual handling of events delegated to specific handlers.
 */
class NeovimEditorSession private constructor(
    private val client: NeovimRpcClient,
    private val scope: CoroutineScope,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
) {
    private val logger = thisLogger()
    private var currentMode = NeovimMode.NORMAL

    companion object {
        suspend fun create(
            client: NeovimRpcClient,
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
        ): NeovimEditorSession? {
            val bufferId =
                createBuffer(client).getOrElse {
                    thisLogger().error("Failed to create buffer: $it")
                    return null
                }
            val documentHandler = NeovimDocumentHandler.create(client, editor, bufferId)
            val cursorHandler = NeovimCursorHandler(client, editor, bufferId)
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

            return session
        }
    }

    fun activateBuffer() {
        scope.launch {
            documentHandler.activateBuffer()
            syncNeovimMode()
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
