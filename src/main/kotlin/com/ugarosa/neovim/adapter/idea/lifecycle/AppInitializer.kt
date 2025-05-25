package com.ugarosa.neovim.adapter.idea.lifecycle

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.ugarosa.neovim.adapter.idea.input.router.NvimKeyRouter
import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.installHook
import com.ugarosa.neovim.rpc.client.api.uiAttach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class AppInitializer(
    scope: CoroutineScope,
) {
    private val logger = myLogger()
    private val client = service<NvimClient>()
    private val optionManager = service<NvimOptionManager>()
    private val keyRouter = service<NvimKeyRouter>()

    init {
        scope.launch {
            client.uiAttach()
            logger.debug("Attached UI")

            client.installHook()
            logger.debug("Installed autocmd hooks")

            optionManager.initializeGlobal()
            logger.debug("Initialized global options")

            keyRouter.start()
            logger.debug("Start Neovim key router")
        }
    }
}
