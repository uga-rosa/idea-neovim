package com.ugarosa.neovim.adapter.coordinator

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.ugarosa.neovim.adapter.idea.editor.CaretPositionSyncAdapter
import com.ugarosa.neovim.adapter.idea.editor.CaretShapeSyncAdapter
import com.ugarosa.neovim.adapter.idea.editor.DocumentSyncAdapter
import com.ugarosa.neovim.adapter.idea.editor.IdeaCaretListener
import com.ugarosa.neovim.adapter.idea.editor.IdeaDocumentListener
import com.ugarosa.neovim.adapter.idea.editor.SelectionSyncAdapter
import com.ugarosa.neovim.adapter.nvim.outgoing.CursorCommandAdapter
import com.ugarosa.neovim.adapter.nvim.outgoing.DocumentCommandAdapter
import com.ugarosa.neovim.bus.IdeaDocumentChange
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.bus.NvimBufLines
import com.ugarosa.neovim.bus.NvimToIdeaBus
import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.domain.buffer.splitDocumentChanges
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.mode.getMode
import com.ugarosa.neovim.domain.mode.setMode
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BufferCoordinator private constructor(
    parentScope: CoroutineScope,
    private val bufferId: BufferId,
    private val editor: EditorEx,
    // Nvim -> Idea adapters
    private val documentSync: DocumentSyncAdapter,
    private val caretPositionSync: CaretPositionSyncAdapter,
    private val caretShapeSync: CaretShapeSyncAdapter,
    private val selectionSync: SelectionSyncAdapter,
    // Idea -> Nvim adapters
    private val documentCommand: DocumentCommandAdapter,
    private val cursorCommand: CursorCommandAdapter,
    // Listeners
    private val caretListener: IdeaCaretListener,
) : Disposable {
    private val logger = myLogger()
    private val optionManager = service<NvimOptionManager>()

    private val ideaToNvimBus = service<IdeaToNvimBus>()
    private val nvimToIdeaBus = service<NvimToIdeaBus>()

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)

    companion object {
        fun getInstance(
            parentScope: CoroutineScope,
            bufferId: BufferId,
            editor: EditorEx,
        ): BufferCoordinator {
            val documentListener = IdeaDocumentListener(bufferId, editor)
            val caretListener = IdeaCaretListener(bufferId, editor)

            documentListener.enable()
            caretListener.enable()

            val buffer =
                BufferCoordinator(
                    parentScope,
                    bufferId,
                    editor,
                    DocumentSyncAdapter(editor, documentListener),
                    CaretPositionSyncAdapter(editor, caretListener),
                    CaretShapeSyncAdapter(editor),
                    SelectionSyncAdapter(editor),
                    DocumentCommandAdapter(bufferId),
                    CursorCommandAdapter(),
                    caretListener,
                )

            Disposer.register(buffer, documentListener)
            Disposer.register(buffer, caretListener)

            return buffer
        }
    }

    init {
        // Initialize the buffer in Neovim
        scope.launch {
            val lines = editor.document.text.replace("\r\n", "\n").split("\n")
            documentCommand.replaceAll(lines)

            val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
            if (virtualFile != null && virtualFile.isInLocalFileSystem) {
                documentCommand.setFiletype(virtualFile.path)
            }

            documentCommand.attach()

            optionManager.initializeLocal(bufferId)
        }

        val documentChanges = mutableListOf<IdeaDocumentChange>()

        // Subscribe to events from Idea
        ideaToNvimBus.documentChange
            .filter { it.bufferId == bufferId }
            .onEach { event ->
                logger.debug("Document change event: $event")
                documentChanges.add(event)
            }
            .launchIn(scope)

        ideaToNvimBus.caretMoved
            .filter { it.bufferId == bufferId }
            .onEach {
                logger.debug("Caret moved event: $it")
                cursorCommand.send(it.bufferId, it.pos)
            }
            .launchIn(scope)

        ideaToNvimBus.editorSelected
            .filter { it.editor == editor }
            .onEach {
                logger.debug("Editor selected event: $it")
                caretShapeSync.apply(bufferId, getMode())
                val pos = caretPositionSync.currentPosition()
                cursorCommand.send(bufferId, pos)
            }
            .launchIn(scope)

        ideaToNvimBus.changeModifiable
            .filter { it.editor == editor }
            .onEach {
                logger.debug("Change modifiable event: $it")
                val isWritable = !editor.isViewer && editor.document.isWritable
                documentCommand.changeModifiable(isWritable)
            }
            .launchIn(scope)

        ideaToNvimBus.escapeInsert
            .filter { it.editor == editor }
            .onEach {
                logger.debug("Escape insert event: $it")

                // Flush document changes before escaping
                val (fixed, block) = splitDocumentChanges(documentChanges)
                fixed.forEach { c ->
                    documentCommand.setText(c)
                }
                block?.let {
                    documentCommand.sendRepeatableChange(block)
                }

                documentCommand.escape()

                // Sync cursor position after escaping
                val curPos = caretPositionSync.currentPosition()
                cursorCommand.send(bufferId, curPos)
            }
            .launchIn(scope)

        // Subscribe to events from Neovim
        nvimToIdeaBus.batchedBufLines
            .forBuffer(bufferId)
            .onEach { events ->
                logger.debug("Received buffer lines: $events")
                val filtered = events.filter { !documentCommand.isIgnored(it.changedTick) }
                if (filtered.isEmpty()) return@onEach
                documentSync.apply(filtered)
            }
            .launchIn(scope)

        nvimToIdeaBus.latestCursor
            .filter { it.bufferId == bufferId }
            .onEach { event ->
                logger.debug("Received cursor event: $event")
                caretPositionSync.apply(event)
            }
            .launchIn(scope)

        nvimToIdeaBus.latestMode
            .filter { it.bufferId == bufferId }
            .onEach { event ->
                logger.debug("Received mode event: $event")
                setMode(event.mode)

                caretShapeSync.apply(bufferId, event.mode)

                if (!event.mode.isInsert()) {
                    withContext(Dispatchers.EDT) {
                        LookupManager.getActiveLookup(editor)?.hideLookup(true)
                    }
                    caretListener.enable()
                } else {
                    caretListener.disable()
                }

                if (!event.mode.isVisualOrSelect()) {
                    selectionSync.reset()
                }
            }
            .launchIn(scope)

        nvimToIdeaBus.latestVisualSelection
            .filter { it.bufferId == bufferId }
            .onEach { event ->
                logger.debug("Received visual selection event: $event")
                selectionSync.apply(event)
            }
            .launchIn(scope)
    }

    private fun Flow<List<NvimBufLines>>.forBuffer(bufferId: BufferId): Flow<List<NvimBufLines>> =
        this.map { list -> list.filter { it.bufferId == bufferId } }
            .filter { it.isNotEmpty() }

    override fun dispose() {
        scope.cancel()
    }
}
