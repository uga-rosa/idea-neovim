package com.ugarosa.neovim.session

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.action.NeovimActionHandler
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeCursorMoveEvent
import com.ugarosa.neovim.rpc.event.maybeExecIdeaActionEvent
import com.ugarosa.neovim.rpc.event.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.event.maybeVisualSelectionEvent
import com.ugarosa.neovim.rpc.function.createBuffer
import com.ugarosa.neovim.selection.NeovimSelectionHandler
import com.ugarosa.neovim.statusline.StatusLineHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

val NEOVIM_SESSION_KEY = Key.create<Deferred<NeovimEditorSession?>>("NEOVIM_SESSION_KEY")

suspend fun Editor.getSession(): NeovimEditorSession {
    return getUserData(NEOVIM_SESSION_KEY)?.await()
        ?: throw IllegalStateException("Neovim session not found")
}

/**
 * Represents a session of Neovim editor.
 * Manages the interaction between Neovim and the IntelliJ editor.
 * Actual handling of events delegated to specific handlers.
 */
class NeovimEditorSession private constructor(
    private val editor: Editor,
    private val bufferId: BufferId,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val statusLineHandler: StatusLineHandler,
    private val actionHandler: NeovimActionHandler,
    private val selectionHandler: NeovimSelectionHandler,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val modeManager = getModeManager()

    companion object {
        fun create(
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
            disposable: Disposable,
        ) {
            editor.putUserData(
                NEOVIM_SESSION_KEY,
                scope.async {
                    createInternal(scope, editor, project, disposable)
                },
            )
        }

        private suspend fun createInternal(
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
            disposable: Disposable,
        ): NeovimEditorSession {
            val bufferId =
                createBuffer(getClient())
                    ?: throw IllegalStateException("Failed to create buffer")
            val documentHandler = NeovimDocumentHandler.create(scope, editor, bufferId)
            val cursorHandler = NeovimCursorHandler.create(scope, editor, disposable, bufferId)
            val statusLineHandler = StatusLineHandler(project)
            val actionHandler = NeovimActionHandler(editor)
            val selectionHandler = NeovimSelectionHandler(editor)
            val session =
                NeovimEditorSession(
                    editor,
                    bufferId,
                    documentHandler,
                    cursorHandler,
                    statusLineHandler,
                    actionHandler,
                    selectionHandler,
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
                documentHandler.applyBufferLinesEvent(event)
            }
        }

        client.registerPushHandler { push ->
            val event = maybeCursorMoveEvent(push)
            if (event?.bufferId == bufferId) {
                cursorHandler.syncNeovimToIdea(event)
            }
        }

        client.registerPushHandler { push ->
            val event = maybeModeChangeEvent(push)
            if (event?.bufferId == bufferId) {
                if (!modeManager.setMode(event.mode)) {
                    logger.trace("No mode change, already in ${event.mode}")
                    return@registerPushHandler
                }
                logger.trace("Change mode to ${event.mode}")

                cursorHandler.changeCursorShape(event.mode)
                statusLineHandler.updateStatusLine(event.mode)

                if (event.mode.isInsert()) {
                    cursorHandler.disableCursorListener()
                } else {
                    // Close completion popup
                    withContext(Dispatchers.EDT) {
                        LookupManager.getActiveLookup(editor)
                            ?.hideLookup(true)
                    }
                    cursorHandler.enableCursorListener()
                }

                if (!event.mode.isVisual()) {
                    selectionHandler.resetSelection()
                }
            }
        }

        client.registerPushHandler { push ->
            val event = maybeExecIdeaActionEvent(push)
            if (event?.bufferId == bufferId) {
                actionHandler.executeAction(event.actionId)
            }
        }

        client.registerPushHandler { push ->
            val event = maybeVisualSelectionEvent(push)
            if (event?.bufferId == bufferId) {
                selectionHandler.applyVisualSelectionEvent(event)
            }
        }
    }

    suspend fun activateBuffer() {
        documentHandler.activateBuffer()
        cursorHandler.syncIdeaToNeovim()
        cursorHandler.changeCursorShape(modeManager.getMode())
    }

    suspend fun executeAction(actionId: String) {
        actionHandler.executeAction(actionId)
    }
}
