package com.ugarosa.neovim.keymap.router

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getKeymapSettings
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.common.setIfDifferent
import com.ugarosa.neovim.config.idea.KeyMappingAction
import com.ugarosa.neovim.config.idea.UserKeyMapping
import com.ugarosa.neovim.keymap.dispatcher.NeovimEventDispatcher
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.rpc.event.NeovimMode
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import com.ugarosa.neovim.rpc.function.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
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
    private val userMappings = CopyOnWriteArrayList<UserKeyMapping>()
    private val currentEditor = AtomicReference<Editor>()

    override fun start() {
        IdeEventQueue.getInstance().addDispatcher(eventDispatcher, this)
        setUserMappings(settings.getUserKeyMappings())
    }

    private fun stop() {
        IdeEventQueue.getInstance().removeDispatcher(eventDispatcher)
        buffer.clear()
    }

    override fun enqueueKey(
        key: NeovimKeyNotation,
        editor: Editor,
    ) {
        // If the editor is different, clear the buffer
        if (currentEditor.setIfDifferent(editor)) {
            logger.trace("Clearing buffer due to editor change")
            buffer.clear()
        }

        buffer.add(key)
        processBuffer(editor)
    }

    private fun processBuffer(editor: Editor) {
        val mode = modeManager.getMode()
        val snapshot = buffer.toList()

        logger.trace("Processing buffer: $snapshot in mode: $mode")

        val prefixMatches =
            userMappings.filter { (modes, lhs) ->
                modes.contains(mode.kind) &&
                    lhs.size >= snapshot.size &&
                    lhs.take(snapshot.size) == snapshot
            }
        val exactlyMatch = prefixMatches.firstOrNull { it.lhs.size == snapshot.size }

        when {
            prefixMatches.isEmpty() -> {
                logger.trace("Fallback to default behavior: $snapshot in mode: $mode")
                scope.launch {
                    fallback(snapshot, editor, mode)
                }
                buffer.clear()
            }

            exactlyMatch != null && prefixMatches.size == 1 -> {
                logger.trace("Executing exact match: $exactlyMatch in mode: $mode")
                scope.launch {
                    executeRhs(exactlyMatch.rhs, editor)
                }
                buffer.clear()
            }

            else -> {
                // pending next input
                logger.trace("Pending next input: $snapshot in mode: $mode")
            }
        }
    }

    private suspend fun fallback(
        keys: List<NeovimKeyNotation>,
        editor: Editor,
        mode: NeovimMode,
    ) {
        if (mode.kind == NeovimModeKind.INSERT) {
            withContext(Dispatchers.EDT) {
                ApplicationManager.getApplication().runWriteAction {
                    val typedAction = TypedAction.getInstance()
                    val dataContext = EditorUtil.getEditorDataContext(editor)
                    keys.forEach { notation ->
                        notation.toSimpleChar()?.let { char ->
                            logger.trace("Typing character: $char")
                            typedAction.actionPerformed(editor, char, dataContext)
                        }
                    }
                }
            }
        } else {
            logger.trace("Sending keys to Neovim: ${keys.joinToString("")}")
            input(client, keys.joinToString(""))
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

    override fun setUserMappings(mappings: List<UserKeyMapping>) {
        userMappings.clear()
        userMappings.addAll(mappings)
        logger.debug("User mappings updated: $userMappings")
    }

    override fun dispose() {
        stop()
    }
}
