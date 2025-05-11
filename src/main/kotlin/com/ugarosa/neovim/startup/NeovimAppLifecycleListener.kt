package com.ugarosa.neovim.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.common.getActionManager
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getCmdlinePopup
import com.ugarosa.neovim.common.getKeyRouter
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.getSessionManager
import com.ugarosa.neovim.rpc.event.createExecIdeaActionCommand
import com.ugarosa.neovim.rpc.event.hookCursorMovedEvent
import com.ugarosa.neovim.rpc.event.hookVisualSelectionEvent
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeCursorMoveEvent
import com.ugarosa.neovim.rpc.event.maybeExecIdeaActionEvent
import com.ugarosa.neovim.rpc.event.maybeVisualSelectionEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeCmdlineEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeRedrawEvent
import com.ugarosa.neovim.rpc.function.uiAttach
import com.ugarosa.neovim.statusline.StatusLineManager
import kotlinx.coroutines.launch

class NeovimAppLifecycleListener : AppLifecycleListener {
    private val logger = thisLogger()
    private val client = getClient()
    private val cmdlinePopup = getCmdlinePopup()
    private val sessionManager = getSessionManager()
    private val actionHandler = getActionManager()

    // Hooks that should be called only once at application startup.
    override fun appFrameCreated(commandLineArgs: List<String>) {
        // You must register the push handlers before calling any other functions
        registerPushHandlers()
        initialize()
    }

    private fun registerPushHandlers() {
        client.registerPushHandler { push ->
            maybeBufLinesEvent(push)?.let { event ->
                val session = sessionManager.getSession(event.bufferId)
                session.handleBufferLinesEvent(event)
            }
        }

        client.registerPushHandler { push ->
            maybeCursorMoveEvent(push)?.let { event ->
                val session = sessionManager.getSession(event.bufferId)
                session.handleCursorMoveEvent(event)
            }
        }

        client.registerPushHandler { push ->
            maybeVisualSelectionEvent(push)?.let { event ->
                val session = sessionManager.getSession(event.bufferId)
                session.handleVisualSelectionEvent(event)
            }
        }

        client.registerPushHandler { push ->
            maybeExecIdeaActionEvent(push)?.let { event ->
                val editor = sessionManager.getEditor(event.bufferId)
                actionHandler.executeAction(event.actionId, editor)
            }
        }

        client.registerPushHandler { push ->
            maybeRedrawEvent(push)?.forEach { event ->
                maybeModeChangeEvent(event)?.let { event ->
                    val session = sessionManager.getSession()
                    session?.handleModeChangeEvent(event)

                    // Update status line
                    focusProject()?.service<StatusLineManager>()?.updateStatusLine()

                    // Close cmdline popup if needed
                    if (!event.mode.isCommand()) {
                        cmdlinePopup.destroy()
                    }
                }
                maybeCmdlineEvent(event)?.let { event ->
                    cmdlinePopup.handleEvent(event)
                }
            }
        }
    }

    private fun initialize() {
        client.scope.launch {
            uiAttach(client)
            logger.debug("Attached UI")

            hookCursorMovedEvent(client)
            logger.debug("Hooked cursor move")

            hookVisualSelectionEvent(client)
            logger.debug("Hooked visual selection event")

            createExecIdeaActionCommand(client)
            logger.debug("Created command")

            val optionManager = getOptionManager()
            optionManager.initializeGlobal()
            logger.debug("Initialized global options")

            val keyRouter = getKeyRouter()
            keyRouter.start()
            logger.trace("Start Neovim key router")
        }
    }
}
