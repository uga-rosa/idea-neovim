package com.ugarosa.neovim.session

import arrow.core.getOrElse
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeCursorMoveEvent
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
    private val bufferId: BufferId,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
) {
    private val logger = thisLogger()
    private var currentMode = NeovimMode.NORMAL

    companion object {
        private val logger = thisLogger()

        suspend fun create(
            client: NeovimRpcClient,
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
            disposable: Disposable,
        ): NeovimEditorSession? {
            val bufferId =
                createBuffer(client).getOrElse {
                    logger.warn("Failed to create buffer: $it")
                    return null
                }
            val documentHandler = NeovimDocumentHandler.create(client, editor, bufferId)
            val cursorHandler = NeovimCursorHandler(client, editor, bufferId, disposable)
            val statusLineHandler = StatusLineHandler(project)
            val session =
                NeovimEditorSession(
                    client,
                    scope,
                    bufferId,
                    documentHandler,
                    cursorHandler,
                    statusLineHandler,
                )

            session.initializePushHandler()

            return session
        }
    }

    private fun initializePushHandler() {
        client.registerPushHandler { push ->
            val event = maybeBufLinesEvent(push)
            if (event?.bufferId == bufferId) {
                logger.trace("Apply buffer lines event: $event")
                documentHandler.applyBufferLinesEvent(event)
            }
        }

        client.registerPushHandler { push ->
            val event = maybeCursorMoveEvent(push)
            if (event?.bufferId == bufferId) {
                logger.trace("Move cursor to $event")
                cursorHandler.syncCursorFromNeovimToIdea(event)
            }
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
            syncNeovimMode()
        }
    }

    fun syncCursorFromIdeaToNeovim() {
        scope.launch {
            cursorHandler.syncCursorFromIdeaToNeovim()
        }
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
