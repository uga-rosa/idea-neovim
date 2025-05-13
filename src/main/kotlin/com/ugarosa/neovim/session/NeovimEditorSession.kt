package com.ugarosa.neovim.session

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.BufLinesEvent
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.event.VisualSelectionEvent
import com.ugarosa.neovim.rpc.event.redraw.ModeChangeEvent
import com.ugarosa.neovim.selection.NeovimSelectionHandler
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
    private val bufferId: BufferId,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val selectionHandler: NeovimSelectionHandler,
) {
    private val logger = myLogger()
    private val undoManager = editor.project?.service<NeovimUndoManager>()

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            disposable: Disposable,
            bufferId: BufferId,
        ): NeovimEditorSession {
            val documentHandler = NeovimDocumentHandler.create(scope, editor, bufferId)
            val cursorHandler = NeovimCursorHandler.create(scope, editor, disposable, bufferId)
            val selectionHandler = NeovimSelectionHandler(editor)
            val session =
                NeovimEditorSession(
                    editor,
                    bufferId,
                    documentHandler,
                    cursorHandler,
                    selectionHandler,
                )

            getOptionManager().initializeLocal(bufferId)

            return session
        }
    }

    fun handleBufferLinesEvent(event: BufLinesEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        documentHandler.applyBufferLinesEvent(event)
    }

    suspend fun handleCursorMoveEvent(event: CursorMoveEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        cursorHandler.syncNeovimToIdea(event)
    }

    suspend fun handleModeChangeEvent(event: ModeChangeEvent) {
        logger.trace("Change mode to ${event.mode}")

        val newMode = event.mode
        val oldMode = getAndSetMode(event.mode)

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

        if (!newMode.isVisual()) {
            selectionHandler.resetSelection()
        }
    }

    suspend fun handleVisualSelectionEvent(event: VisualSelectionEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        selectionHandler.applyVisualSelectionEvent(event)
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
