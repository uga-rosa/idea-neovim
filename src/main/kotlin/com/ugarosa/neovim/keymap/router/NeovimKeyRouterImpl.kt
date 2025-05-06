package com.ugarosa.neovim.keymap.router

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getModeManager
import com.ugarosa.neovim.config.idea.KeyMappingAction
import com.ugarosa.neovim.config.idea.UserKeyMapping
import com.ugarosa.neovim.keymap.dispatcher.NeovimEventDispatcher
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.rpc.event.NeovimMode
import com.ugarosa.neovim.rpc.event.NeovimModeKind
import com.ugarosa.neovim.rpc.function.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimKeyRouterImpl(
    private val scope: CoroutineScope,
) : NeovimKeyRouter, Disposable {
    private val logger = thisLogger()

    private val eventDispatcher = NeovimEventDispatcher()
    private val client = getClient()
    private val modeManager = getModeManager()

    private val buffer = ConcurrentLinkedDeque<NeovimKeyNotation>()
    private val userMappings = CopyOnWriteArrayList<UserKeyMapping>()
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
    ) {
        // If the editor is different, clear the buffer
        val prevEditor = currentEditor.getAndSet(editor)
        if (prevEditor != editor) {
            buffer.clear()
        }

        buffer.add(key)
        processBuffer(editor)
    }

    private fun processBuffer(editor: Editor) {
        val mode = modeManager.getMode()
        val snapshot = buffer.toList()

        val prefixMatches =
            userMappings.filter { (modes, lhs) ->
                modes.contains(mode.kind) &&
                    lhs.size >= snapshot.size &&
                    lhs.take(snapshot.size) == snapshot
            }
        val exactlyMatch = prefixMatches.firstOrNull { it.lhs.size == snapshot.size }

        when {
            prefixMatches.isEmpty() -> {
                // Fallback to default behavior
                fallback(snapshot, editor, mode)
                buffer.clear()
            }

            exactlyMatch != null && prefixMatches.size == 1 -> {
                executeRhs(exactlyMatch.rhs, editor)
                buffer.clear()
            }

            prefixMatches.size > 1 -> {
                // pending next input
            }
        }
    }

    private fun fallback(
        keys: List<NeovimKeyNotation>,
        editor: Editor,
        mode: NeovimMode,
    ) {
        if (mode.kind == NeovimModeKind.INSERT) {
            val typedAction = TypedAction.getInstance()
            val dataContext = EditorUtil.getEditorDataContext(editor)
            ApplicationManager.getApplication().runWriteAction {
                keys.forEach { notation ->
                    notation.toSimpleChar()?.let { char ->
                        typedAction.actionPerformed(editor, char, dataContext)
                    }
                }
            }
        } else {
            scope.launch {
                input(client, keys.joinToString(""))
            }
        }
    }

    private fun executeRhs(
        actions: List<KeyMappingAction>,
        editor: Editor,
    ) {
        scope.launch {
            actions.forEach { action ->
                when (action) {
                    is KeyMappingAction.SendToNeovim -> {
                        input(client, action.key.toString())
                    }

                    is KeyMappingAction.ExecuteIdeaAction -> {
                        callAction(action, editor)
                    }
                }
            }
        }
    }

    private fun callAction(
        action: KeyMappingAction.ExecuteIdeaAction,
        editor: Editor,
    ) {
        val action =
            ActionManager.getInstance().getAction(action.actionId)
                ?: run {
                    logger.warn("Action not found: ${action.actionId}")
                    return
                }
        val res =
            ActionManager.getInstance().tryToExecute(
                action,
                null,
                editor.contentComponent,
                "IdeaNeovim",
                true,
            )
        res.waitFor(5_000)
    }

    override fun setUserMappings(mappings: List<UserKeyMapping>) {
        userMappings.clear()
        userMappings.addAll(mappings)
    }

    override fun dispose() {
        stop()
    }
}
