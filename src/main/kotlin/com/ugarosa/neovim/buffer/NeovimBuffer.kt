package com.ugarosa.neovim.buffer

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.buffer.caret.NeovimCaretHandler
import com.ugarosa.neovim.buffer.document.NeovimDocumentHandler
import com.ugarosa.neovim.buffer.selection.NeovimSelectionHandler
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.localHooks
import com.ugarosa.neovim.rpc.client.api.winSetBuf
import com.ugarosa.neovim.rpc.event.handler.BufLinesEvent
import com.ugarosa.neovim.rpc.event.handler.CursorMoveEvent
import com.ugarosa.neovim.rpc.type.NeovimRegion
import com.ugarosa.neovim.window.WindowId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimBuffer private constructor(
    private val bufferId: BufferId,
    private val editor: EditorEx,
    private val documentHandler: NeovimDocumentHandler,
    private val caretHandler: NeovimCaretHandler,
    private val selectionHandler: NeovimSelectionHandler,
) {
    private val logger = myLogger()
    private val client = service<NeovimClient>()

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            bufferId: BufferId,
            editor: EditorEx,
        ): NeovimBuffer {
            val documentHandler = NeovimDocumentHandler.create(scope, bufferId, editor)
            val caretHandler = NeovimCaretHandler.create(scope, bufferId, editor)
            val selectionHandler = NeovimSelectionHandler(editor)
            val buffer =
                NeovimBuffer(
                    bufferId,
                    editor,
                    documentHandler,
                    caretHandler,
                    selectionHandler,
                )

            val optionManager = service<NeovimOptionManager>()
            optionManager.initializeLocal(bufferId)

            val client = service<NeovimClient>()
            client.localHooks(bufferId)

            return buffer
        }
    }

    suspend fun handleBufferLinesEvent(event: BufLinesEvent) {
        documentHandler.applyBufferLinesEvent(event)
    }

    suspend fun handleCursorMoveEvent(event: CursorMoveEvent) {
        caretHandler.syncNeovimToIdea(event)
    }

    suspend fun handleModeChangeEvent(mode: NeovimMode) {
        logger.trace("Change mode to $mode")

        val oldMode = getAndSetMode(mode)

        caretHandler.changeCursorShape(oldMode, mode)

        if (mode.isInsert()) {
            caretHandler.disableCaretListener()
        } else {
            // Close completion popup
            withContext(Dispatchers.EDT) {
                LookupManager.getActiveLookup(editor)
                    ?.hideLookup(true)
            }
            caretHandler.enableCaretListener()
        }

        if (!mode.isVisualOrSelect()) {
            selectionHandler.resetSelection()
        }
    }

    suspend fun handleVisualSelectionEvent(regions: List<NeovimRegion>) {
        selectionHandler.applyVisualSelectionEvent(regions)
    }

    suspend fun setWindow(windowId: WindowId) {
        client.winSetBuf(windowId, bufferId)
        caretHandler.setWindow(windowId)
    }

    suspend fun onSelected() {
        caretHandler.syncIdeaToNeovim()
        val mode = getMode()
        caretHandler.changeCursorShape(mode, mode)
    }

    suspend fun changeModifiable() {
        documentHandler.changeModifiable()
    }
}
