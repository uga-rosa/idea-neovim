package com.ugarosa.neovim.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.neovim.NeovimOptionManager
import com.ugarosa.neovim.keymap.router.NeovimKeyRouter
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.globalHooks
import com.ugarosa.neovim.rpc.client.api.uiAttach
import com.ugarosa.neovim.rpc.event.handler.onBufLinesEvent
import com.ugarosa.neovim.rpc.event.handler.onCursorMoveEvent
import com.ugarosa.neovim.rpc.event.handler.onExecIdeaActionEvent
import com.ugarosa.neovim.rpc.event.handler.onModeChangeEventCustom
import com.ugarosa.neovim.rpc.event.handler.onOptionSetEvent
import com.ugarosa.neovim.rpc.event.handler.onRedrawEvent
import com.ugarosa.neovim.rpc.event.handler.onVisualSelectionEvent
import kotlinx.coroutines.launch

class NeovimAppLifecycleListener : AppLifecycleListener {
    private val logger = myLogger()
    private val client = service<NeovimClient>()
    private val optionManager = service<NeovimOptionManager>()
    private val keyRouter = service<NeovimKeyRouter>()

    // Hooks that should be called only once at application startup.
    override fun appFrameCreated(commandLineArgs: List<String>) {
        registerNotificationHandlers()
        initialize()
    }

    private fun registerNotificationHandlers() {
        // Handle Neovim Native events
        onRedrawEvent(client)
        onBufLinesEvent(client)
        // Handle Custom events
        onCursorMoveEvent(client)
        onModeChangeEventCustom(client)
        onVisualSelectionEvent(client)
        onOptionSetEvent(client)
        onExecIdeaActionEvent(client)
    }

    private fun initialize() {
        client.scope.launch {
            client.uiAttach()
            logger.debug("Attached UI")

            client.globalHooks()
            logger.debug("Registered global hooks")

            optionManager.initializeGlobal()
            logger.debug("Initialized global options")

            keyRouter.start()
            logger.debug("Start Neovim key router")
        }
    }
}
