package com.ugarosa.neovim.rpc.process

import com.intellij.openapi.diagnostic.thisLogger

class AutoNeovimProcessManager : NeovimProcessManager {
    private val logger = thisLogger()

    private val delegate: NeovimProcessManager =
        run {
            val addr = System.getProperty("nvim.listen.address")
            if (addr != null) {
                logger.debug("Connecting to Neovim at $addr")
                SocketProcessManager(addr)
            } else {
                logger.debug("Starting Neovim in embedded mode")
                EmbedProcessManager()
            }
        }

    override fun getInputStream() = delegate.getInputStream()

    override fun getOutputStream() = delegate.getOutputStream()

    override fun close() = delegate.close()
}
