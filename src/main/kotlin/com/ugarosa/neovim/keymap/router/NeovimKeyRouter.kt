package com.ugarosa.neovim.keymap.router

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.action.NeovimActionManager
import com.ugarosa.neovim.config.idea.KeyMappingAction
import com.ugarosa.neovim.config.idea.NeovimKeymapSettings
import com.ugarosa.neovim.keymap.dispatcher.NeovimKeyEventDispatcher
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimKeyRouter(
    private val scope: CoroutineScope,
) : Disposable {
    private val logger = myLogger()

    private val dispatcher = NeovimKeyEventDispatcher(this)
    private val client = service<NeovimClient>()
    private val settings = service<NeovimKeymapSettings>()
    private val actionHandler = service<NeovimActionManager>()

    private val buffer = ConcurrentLinkedDeque<NeovimKeyNotation>()
    private val currentEditor = AtomicReference<Editor?>()

    fun start() {
        IdeEventQueue.getInstance().addDispatcher(dispatcher, this)
    }

    private fun stop() {
        IdeEventQueue.getInstance().removeDispatcher(dispatcher)
        buffer.clear()
    }

    fun enqueueKey(
        key: NeovimKeyNotation,
        editor: Editor?,
    ): Boolean {
        // If the editor is different, clear the buffer
        val oldEditor = currentEditor.getAndUpdate { if (it == editor) it else editor }
        if (oldEditor != editor) {
            logger.trace("Clearing buffer due to editor change")
            buffer.clear()
        }

        buffer.add(key)
        return processBuffer(editor)
    }

    private fun processBuffer(editor: Editor?): Boolean {
        val mode = getMode()
        val snapshot = buffer.toList()

        logger.trace("Processing buffer: $snapshot in mode: $mode")

        val prefixMatches =
            settings.getUserKeyMappings().filter { (mapMode, lhs) ->
                mapMode.toModeKinds().contains(mode.kind) &&
                    lhs.size >= snapshot.size &&
                    lhs.take(snapshot.size) == snapshot
            }
        val exactlyMatch = prefixMatches.firstOrNull { it.lhs.size == snapshot.size }

        when {
            // No match found.
            prefixMatches.isEmpty() -> {
                buffer.clear()
                // Don't consume the key if the mode is insert-mode
                if (mode.isInsert()) {
                    logger.trace("No match found, but in insert mode: $mode")
                    return false
                }
                scope.launch {
                    logger.trace("No match found, sending keys to Neovim: $snapshot")
                    client.input(snapshot.joinToString(separator = ""))
                }
                return true
            }

            // Exact match found.
            exactlyMatch != null && prefixMatches.size == 1 -> {
                buffer.clear()
                logger.trace("Executing exact match: $exactlyMatch in mode: $mode")
                scope.launch {
                    executeRhs(exactlyMatch.rhs, editor)
                }
                return true
            }

            // Some prefix match found. Pending next input.
            else -> {
                logger.trace("Pending next input: $snapshot in mode: $mode")
                return true
            }
        }
    }

    private suspend fun executeRhs(
        actions: List<KeyMappingAction>,
        editor: Editor?,
    ) {
        actions.forEach { action ->
            when (action) {
                is KeyMappingAction.SendToNeovim -> {
                    logger.trace("Sending key to Neovim: ${action.key}")
                    client.input(action.key.toString())
                }

                is KeyMappingAction.ExecuteIdeaAction -> {
                    logger.trace("Executing action: ${action.actionId}")
                    actionHandler.executeAction(action.actionId, editor)
                }
            }
        }
    }

    override fun dispose() {
        stop()
    }
}
