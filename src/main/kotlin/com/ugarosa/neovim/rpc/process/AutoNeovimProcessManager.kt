package com.ugarosa.neovim.rpc.process

import com.intellij.openapi.diagnostic.thisLogger

class AutoNeovimProcessManager : NeovimProcessManager {
    private val delegate: NeovimProcessManager =
        run {
            val addr = System.getProperty("nvim.listen.address")
            thisLogger().warn("Neovim address: $addr")
            if (addr != null) {
                SocketProcessManager(addr)
            } else {
                EmbedProcessManager()
            }
        }

    override fun getInputStream() = delegate.getInputStream()

    override fun getOutputStream() = delegate.getOutputStream()

    override fun close() = delegate.close()
}
