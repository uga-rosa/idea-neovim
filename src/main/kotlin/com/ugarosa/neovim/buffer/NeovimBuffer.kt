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
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class NeovimBuffer private constructor(
    scope: CoroutineScope,
    private val editor: EditorEx,
    private val documentHandler: NeovimDocumentHandler,
    private val cursorHandler: NeovimCursorHandler,
    private val selectionHandler: NeovimSelectionHandler,
) : Disposable {
    private val logger = myLogger()
    private val batchWindowMs = 50L
    private val channel = Channel<Event>(Channel.UNLIMITED)

    private sealed interface Event {
        data class Buffer(val event: BufLinesEvent) : Event

        data class Cursor(val event: CursorMoveEvent) : Event
    }

    init {
        Disposer.register(this, documentHandler)
        Disposer.register(this, cursorHandler)
        Disposer.register(this, selectionHandler)

        scope.launch {
            val bufferBucket = mutableListOf<BufLinesEvent>()
            val cursorBucket = mutableListOf<CursorMoveEvent>()

            suspend fun flushBuffer() {
                if (bufferBucket.isEmpty()) return
                documentHandler.handleBufferLinesEvents(bufferBucket)
                bufferBucket.clear()
            }

            suspend fun flushCursor() {
                if (cursorBucket.isEmpty()) return
                val latestEvent = cursorBucket.last()
                cursorHandler.handleCursorMoveEvent(latestEvent)
                cursorBucket.clear()
            }

            for (event in channel) {
                when (event) {
                    is Event.Buffer -> bufferBucket.add(event.event)
                    is Event.Cursor -> cursorBucket.add(event.event)
                }

                while (true) {
                    val next = withTimeoutOrNull(batchWindowMs) { channel.receive() }
                    if (next == null) break
                    when (next) {
                        is Event.Buffer -> bufferBucket.add(next.event)
                        is Event.Cursor -> cursorBucket.add(next.event)
                    }
                }
                flushBuffer()
                flushCursor()
            }
        }
    }

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            bufferId: BufferId,
            editor: EditorEx,
        ): NeovimBuffer {
            @OptIn(ObsoleteCoroutinesApi::class)
            val sendChan =
                scope.actor<suspend () -> Unit>(capacity = Channel.UNLIMITED) {
                    for (lambda in channel) {
                        lambda()
                    }
                }

            val documentHandler = NeovimDocumentHandler.create(bufferId, editor, sendChan)
            val cursorHandler = NeovimCursorHandler.create(bufferId, editor, sendChan)
            val selectionHandler = NeovimSelectionHandler(editor)
            val buffer =
                NeovimBuffer(
                    scope,
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

    suspend fun onBufferLinesEvent(event: BufLinesEvent) {
        channel.send(Event.Buffer(event))
    }

    suspend fun onCursorMoveEvent(event: CursorMoveEvent) {
        channel.send(Event.Cursor(event))
    }

    suspend fun handleModeChangeEvent(mode: NeovimMode) {
        logger.trace("Change mode to $mode")

        val oldMode = getAndSetMode(mode)

        cursorHandler.changeCursorShape(oldMode, mode)

        if (!mode.isInsert()) {
            // Close completion popup
            withContext(Dispatchers.EDT) {
                LookupManager.getActiveLookup(editor)
                    ?.hideLookup(true)
            }
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

    override fun dispose() {}
}
