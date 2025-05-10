package com.ugarosa.neovim.session

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.action.NeovimActionHandler
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.cursor.NeovimCursorHandler
import com.ugarosa.neovim.document.NeovimDocumentHandler
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.BufLinesEvent
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.event.ExecIdeaActionEvent
import com.ugarosa.neovim.rpc.event.ModeChangeEvent
import com.ugarosa.neovim.rpc.event.VisualSelectionEvent
import com.ugarosa.neovim.selection.NeovimSelectionHandler
import com.ugarosa.neovim.statusline.StatusLineHandler
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
    private val statusLineHandler: StatusLineHandler,
    private val actionHandler: NeovimActionHandler,
    private val selectionHandler: NeovimSelectionHandler,
) {
    private val logger = thisLogger()
    private val modeManager = getModeManager()

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            project: Project,
            disposable: Disposable,
            bufferId: BufferId,
        ): NeovimEditorSession {
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

            getOptionManager().initializeLocal(bufferId)

            return session
        }
    }

    suspend fun handleBufferLinesEvent(event: BufLinesEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        documentHandler.applyBufferLinesEvent(event)
    }

    suspend fun handleCursorMoveEvent(event: CursorMoveEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        cursorHandler.syncNeovimToIdea(event)
    }

    suspend fun handleModeChangeEvent(event: ModeChangeEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }

        if (!modeManager.setMode(event.mode)) {
            logger.trace("No mode change, already in ${event.mode}")
            return
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

    suspend fun handleVisualSelectionEvent(event: VisualSelectionEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        selectionHandler.applyVisualSelectionEvent(event)
    }

    suspend fun handleExecIdeaActionEvent(event: ExecIdeaActionEvent) {
        require(event.bufferId == bufferId) { "Buffer ID mismatch" }
        actionHandler.executeAction(event.actionId)
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
