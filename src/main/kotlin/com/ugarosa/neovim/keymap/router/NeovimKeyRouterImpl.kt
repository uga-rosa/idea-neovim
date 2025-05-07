package com.ugarosa.neovim.keymap.router

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getKeymapSettings
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.common.setIfDifferent
import com.ugarosa.neovim.config.idea.KeyMappingAction
import com.ugarosa.neovim.keymap.dispatcher.NeovimEventDispatcher
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.rpc.function.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimKeyRouterImpl(
    private val scope: CoroutineScope,
) : NeovimKeyRouter, Disposable {
    private val logger = thisLogger()

    private val eventDispatcher = NeovimEventDispatcher(this)
    private val client = getClient()
    private val modeManager = getModeManager()
    private val settings = getKeymapSettings()

    private val buffer = ConcurrentLinkedDeque<NeovimKeyNotation>()
    private val currentEditor = AtomicReference<Editor>()

    override fun start() {
        IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this)
    }

    private fun stop() {
        IdeEventQueue.getInstance().removeDispatcher(eventDispatcher)
        buffer.clear()
    }

    override fun enqueueKey(
        key: NeovimKeyNotation,
        editor: Editor,
    ): Boolean {
        // If the editor is different, clear the buffer
        if (currentEditor.setIfDifferent(editor)) {
            logger.trace("Clearing buffer due to editor change")
            buffer.clear()
        }

        buffer.add(key)
        return processBuffer(editor)
    }

    private fun processBuffer(editor: Editor): Boolean {
        val mode = modeManager.getMode()
        val snapshot = buffer.toList()

        logger.trace("Processing buffer: $snapshot in mode: $mode")

        val prefixMatches =
            settings.getUserKeyMappings().filter { (modes, lhs) ->
                modes.contains(mode.kind) &&
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
                    input(client, snapshot.joinToString(separator = ""))
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
        editor: Editor,
    ) {
        actions.forEach { action ->
            when (action) {
                is KeyMappingAction.SendToNeovim -> {
                    logger.trace("Sending key to Neovim: ${action.key}")
                    input(client, action.key.toString())
                }

                is KeyMappingAction.ExecuteIdeaAction -> {
                    logger.trace("Executing action: ${action.actionId}")
                    callAction(action, editor)
                }
            }
        }
    }

    private suspend fun callAction(
        action: KeyMappingAction.ExecuteIdeaAction,
        editor: Editor,
    ) {
        val anAction =
            ActionManager.getInstance().getAction(action.actionId)
                ?: run {
                    logger.warn("Action not found: ${action.actionId}")
                    return
                }
        withContext(Dispatchers.EDT) {
            val res =
                ActionManager.getInstance().tryToExecute(
                    anAction,
                    null,
                    editor.contentComponent,
                    "IdeaNeovim",
                    true,
                )
            res.waitFor(5_000)
        }
    }

    override fun dispose() {
        stop()
    }
}
