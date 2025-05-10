package com.ugarosa.neovim.rpc.function

import java.util.concurrent.atomic.AtomicReference

// The channel ID is obtained from Neovim as a health check during startup.
object ChanIdManager {
    private val chanId = AtomicReference<Int>()

    fun set(id: Int) {
        chanId.set(id)
    }

    fun get(): Int {
        return chanId.get() ?: error("Channel ID not set")
    }
}
