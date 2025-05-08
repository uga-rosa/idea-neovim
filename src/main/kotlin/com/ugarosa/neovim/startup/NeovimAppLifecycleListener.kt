package com.ugarosa.neovim.startup

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getKeyRouter
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.rpc.function.createCommand
import com.ugarosa.neovim.rpc.function.enforceSingleWindow
import com.ugarosa.neovim.rpc.function.hookCursorMove
import com.ugarosa.neovim.rpc.function.hookModeChange
import kotlinx.coroutines.launch

class NeovimAppLifecycleListener : AppLifecycleListener {
    private val logger = thisLogger()

    // Hooks that should be called only once at application startup.
    override fun appFrameCreated(commandLineArgs: List<String>) {
        val client = getClient()
        val optionManager = getOptionManager()
        val keyRouter = getKeyRouter()

        client.scope.launch {
            enforceSingleWindow(client).onLeft {
                logger.warn("Failed to enforce single window: $it")
            }.onRight {
                logger.debug("Enforced single window")
            }

            hookCursorMove(client).onLeft {
                logger.warn("Failed to hook cursor move: $it")
            }.onRight {
                logger.debug("Hooked cursor move")
            }

            hookModeChange(client).onLeft {
                logger.warn("Failed to hook mode change: $it")
            }.onRight {
                logger.debug("Hooked mode change")
            }

            createCommand(client).onLeft {
                logger.warn("Failed to create command: $it")
            }.onRight {
                logger.debug("Created command")
            }

            optionManager.initializeGlobal()
        }

        logger.trace("Starting Neovim key router")
        keyRouter.start()
    }
}
