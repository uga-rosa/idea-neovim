package com.ugarosa.neovim.session

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.localHooks
import com.ugarosa.neovim.rpc.event.handler.BufLinesEvent
import com.ugarosa.neovim.rpc.event.handler.CursorMoveEvent
import com.ugarosa.neovim.rpc.type.NeovimObject
import com.ugarosa.neovim.rpc.type.NeovimRegion
import com.ugarosa.neovim.session.cursor.NeovimCursorHandler
import com.ugarosa.neovim.session.document.NeovimDocumentHandler
import com.ugarosa.neovim.session.selection.NeovimSelectionHandler
import com.ugarosa.neovim.undo.NeovimUndoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a session of Neovim editor.
 * Manages the interaction between Neovim and the IntelliJ editor.
 * Actual handling of events delegated to specific handlers.
 */
class NeovimEditorSession private constructor(
    private val editor: Editor,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val selectionHandler: NeovimSelectionHandler,
) {
    private val logger = myLogger()
    private val undoManager = editor.project?.service<NeovimUndoManager>()

    companion object {
        private val client = service<NeovimClient>()
        private val optionManager = service<NeovimOptionManager>()

        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            disposable: Disposable,
            bufferId: NeovimObject.BufferId,
        ): NeovimEditorSession {
            val documentHandler = NeovimDocumentHandler.create(scope, editor, bufferId)
            val cursorHandler = NeovimCursorHandler.create(scope, editor, disposable, bufferId)
            val selectionHandler = NeovimSelectionHandler(editor)
            val session =
                NeovimEditorSession(
                    editor,
                    documentHandler,
                    cursorHandler,
                    selectionHandler,
                )

            optionManager.initializeLocal(bufferId)

            client.localHooks(bufferId)

            return session
        }
    }

    fun handleBufferLinesEvent(event: BufLinesEvent) {
        documentHandler.applyBufferLinesEvent(event)
    }

    suspend fun handleCursorMoveEvent(event: CursorMoveEvent) {
        cursorHandler.syncNeovimToIdea(event)
    }

    suspend fun handleModeChangeEvent(mode: NeovimMode) {
        logger.trace("Change mode to $mode")

        val newMode = mode
        val oldMode = getAndSetMode(newMode)

        cursorHandler.changeCursorShape(oldMode, newMode)

        if (oldMode.isInsert() && !newMode.isInsert()) {
            undoManager?.setCheckpoint()
        }

        if (newMode.isInsert()) {
            cursorHandler.disableCursorListener()
        } else {
            // Close completion popup
            withContext(Dispatchers.EDT) {
                LookupManager.getActiveLookup(editor)
                    ?.hideLookup(true)
            }
            cursorHandler.enableCursorListener()
        }

        if (!newMode.isVisualOrSelect()) {
            selectionHandler.resetSelection()
        }
    }

    suspend fun handleVisualSelectionEvent(regions: List<NeovimRegion>) {
        selectionHandler.applyVisualSelectionEvent(regions)
    }

    suspend fun activateBuffer() {
        documentHandler.activateBuffer()
        cursorHandler.syncIdeaToNeovim()
        val mode = getMode()
        cursorHandler.changeCursorShape(mode, mode)
    }

    suspend fun changeModifiable() {
        documentHandler.changeModifiable()
    }
}
