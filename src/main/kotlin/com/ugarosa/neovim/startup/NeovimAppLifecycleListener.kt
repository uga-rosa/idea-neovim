package com.ugarosa.neovim.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getKeyRouter
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.rpc.event.createExecIdeaActionCommand
import com.ugarosa.neovim.rpc.event.hookCursorMove
import com.ugarosa.neovim.rpc.event.hookModeChange
import com.ugarosa.neovim.rpc.function.enforceSingleWindow
import kotlinx.coroutines.launch

class NeovimAppLifecycleListener : AppLifecycleListener {
    private val logger = thisLogger()

    // Hooks that should be called only once at application startup.
    override fun appFrameCreated(commandLineArgs: List<String>) {
        val client = getClient()

        client.scope.launch {
            enforceSingleWindow(client)
            logger.debug("Enforced single window")

            hookCursorMove(client)
            logger.debug("Hooked cursor move")

            hookModeChange(client)
            logger.debug("Hooked mode change")

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
