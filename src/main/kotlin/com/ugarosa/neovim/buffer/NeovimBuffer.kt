package com.ugarosa.neovim.buffer

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Disposer
import com.ugarosa.neovim.buffer.cursor.NeovimCursorHandler
import com.ugarosa.neovim.buffer.document.NeovimDocumentHandler
import com.ugarosa.neovim.buffer.selection.NeovimSelectionHandler
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.localHooks
import com.ugarosa.neovim.rpc.event.handler.BufLinesEvent
import com.ugarosa.neovim.rpc.event.handler.CursorMoveEvent
import com.ugarosa.neovim.rpc.type.NeovimRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimBuffer private constructor(
    private val editor: EditorEx,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val selectionHandler: NeovimSelectionHandler,
) : Disposable {
    private val logger = myLogger()

    init {
        Disposer.register(this, documentHandler)
        Disposer.register(this, cursorHandler)
        Disposer.register(this, selectionHandler)
    }

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            bufferId: BufferId,
            editor: EditorEx,
        ): NeovimBuffer {
            val documentHandler = NeovimDocumentHandler.create(scope, bufferId, editor)
            val cursorHandler = NeovimCursorHandler.create(scope, bufferId, editor)
            val selectionHandler = NeovimSelectionHandler(editor)
            val buffer =
                NeovimBuffer(
                    editor,
                    documentHandler,
                    cursorHandler,
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
        cursorHandler.syncNeovimToIdea(event)
    }

    suspend fun handleModeChangeEvent(mode: NeovimMode) {
        logger.trace("Change mode to $mode")

        val oldMode = getAndSetMode(mode)

        cursorHandler.changeCursorShape(oldMode, mode)

        if (mode.isInsert()) {
            cursorHandler.disableListener()
        } else {
            // Close completion popup
            withContext(Dispatchers.EDT) {
                LookupManager.getActiveLookup(editor)
                    ?.hideLookup(true)
            }
            cursorHandler.enableListener()
        }

        if (!mode.isVisualOrSelect()) {
            selectionHandler.resetSelection()
        }
    }

    suspend fun handleVisualSelectionEvent(regions: List<NeovimRegion>) {
        selectionHandler.applyVisualSelectionEvent(regions)
    }

    suspend fun onSelected() {
        cursorHandler.syncIdeaToNeovim()
        val mode = getMode()
        cursorHandler.changeCursorShape(mode, mode)
    }

    suspend fun changeModifiable() {
        documentHandler.changeModifiable()
    }

    override fun dispose() { }
}
