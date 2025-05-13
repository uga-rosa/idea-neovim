package com.ugarosa.neovim.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.common.getActionManager
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getCmdlinePopup
import com.ugarosa.neovim.common.getKeyRouter
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.getSessionManager
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getAndSetMode
import com.ugarosa.neovim.rpc.event.OptionScope
import com.ugarosa.neovim.rpc.event.globalHooks
import com.ugarosa.neovim.rpc.event.maybeBufLinesEvent
import com.ugarosa.neovim.rpc.event.maybeCursorMoveEvent
import com.ugarosa.neovim.rpc.event.maybeExecIdeaActionEvent
import com.ugarosa.neovim.rpc.event.maybeModeChangeEventCustom
import com.ugarosa.neovim.rpc.event.maybeOptionSetEvent
import com.ugarosa.neovim.rpc.event.maybeVisualSelectionEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeCmdlineEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeModeChangeEvent
import com.ugarosa.neovim.rpc.event.redraw.maybeRedrawEvent
import com.ugarosa.neovim.rpc.function.uiAttach
import com.ugarosa.neovim.statusline.StatusLineManager
import kotlinx.coroutines.launch

class NeovimAppLifecycleListener : AppLifecycleListener {
    private val logger = myLogger()
    private val client = getClient()
    private val optionManager = getOptionManager()
    private val cmdlinePopup = getCmdlinePopup()
    private val sessionManager = getSessionManager()
    private val actionManager = getActionManager()
    private val keyRouter = getKeyRouter()

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
                actionManager.executeAction(event.actionId, editor)
            }
        }

        client.registerPushHandler { push ->
            maybeModeChangeEventCustom(push)?.let { mode ->
                getAndSetMode(mode)
            }
        }

        client.registerPushHandler { push ->
            maybeRedrawEvent(push)?.forEach { redraw ->
                maybeModeChangeEvent(redraw)?.let { event ->
                    val session = sessionManager.getSession()
                    session?.handleModeChangeEvent(event)

                    // Update status line
                    focusProject()?.service<StatusLineManager>()?.updateStatusLine()

                    // Close cmdline popup if needed
                    if (!event.mode.isCommand()) {
                        cmdlinePopup.destroy()
                    }
                }
                maybeCmdlineEvent(redraw)?.let { event ->
                    cmdlinePopup.handleEvent(event)
                }
            }
        }

        client.registerPushHandler { push ->
            maybeOptionSetEvent(push)?.let { event ->
                logger.trace("Received an option set event: $event")
                when (event.scope) {
                    OptionScope.LOCAL -> optionManager.putLocal(event.bufferId, event.name, event.value)
                    OptionScope.GLOBAL -> optionManager.putGlobal(event.name, event.value)
                }
            }
        }
    }

    private fun initialize() {
        client.scope.launch {
            uiAttach(client)
            logger.debug("Attached UI")

            globalHooks(client)
            logger.debug("Registered global hooks")

            optionManager.initializeGlobal()
            logger.debug("Initialized global options")

            keyRouter.start()
            logger.debug("Start Neovim key router")
        }
    }
}
