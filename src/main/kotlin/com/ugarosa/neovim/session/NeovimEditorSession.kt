package com.ugarosa.neovim.session

import arrow.core.getOrElse
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.rd.util.AtomicReference
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.setIfDifferent
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.NeovimMode
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeCursorMoveEvent
import com.ugarosa.neovim.rpc.event.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.function.createBuffer
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
    private val scope: CoroutineScope,
    private val bufferId: BufferId,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val mode = AtomicReference(NeovimMode.default)

    companion object {
        private val logger = thisLogger()

        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
            disposable: Disposable,
        ): NeovimEditorSession? {
            val bufferId =
                createBuffer(getClient()).getOrElse {
                    logger.warn("Failed to create buffer: $it")
                    return null
                }
            val documentHandler = NeovimDocumentHandler.create(editor, bufferId)
            val cursorHandler = NeovimCursorHandler(editor, bufferId, disposable)
            val statusLineHandler = StatusLineHandler(project)
            val session =
                NeovimEditorSession(
                    scope,
                    bufferId,
                    documentHandler,
                    cursorHandler,
                    statusLineHandler,
                )

            session.initializePushHandler()

            getOptionManager().initializeLocal(bufferId)

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
                logger.trace("Apply cursor move event: $event")
                cursorHandler.syncNeovimToIdea(event)
            }
        }

        client.registerPushHandler { push ->
            val event = maybeModeChangeEvent(push)
            if (event?.bufferId == bufferId) {
                if (mode.setIfDifferent(event.mode)) {
                    logger.trace("Change mode to ${event.mode}")
                    cursorHandler.changeCursorShape(event.mode)
                    statusLineHandler.updateStatusLine(event.mode)

                    // Disable nvim_buf_lines_event if in insert mode
                    if (event.mode.kind == NeovimModeKind.INSERT) {
                        documentHandler.disableBufLinesEvent()
                        cursorHandler.disableCursorListener()
                        // CursorMoveEvent is not sent in insert mode, so I need to sync it manually
                    } else {
                        documentHandler.enableBufLinesEvent()
                        cursorHandler.enableCursorListener()
                    }
                } else {
                    logger.debug("No mode change, already in ${event.mode}")
                }
            }
        }
    }

    fun activateBuffer() {
        scope.launch {
            documentHandler.activateBuffer()
            cursorHandler.syncIdeaToNeovim()
        }
    }

    fun sendKeyAndSyncStatus(key: String) {
        scope.launch {
            input(client, key)
        }
    }

    fun syncCursorFromIdeaToNeovim() {
        scope.launch {
            cursorHandler.syncIdeaToNeovim()
        }
    }

    fun getMode(): NeovimMode {
        return mode.get()
    }
}
