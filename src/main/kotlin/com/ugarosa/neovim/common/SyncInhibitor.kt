package com.ugarosa.neovim.common

import arrow.atomic.AtomicBoolean

class SyncInhibitor(initiallyInhibited: Boolean = false) {
    private val active = AtomicBoolean(!initiallyInhibited)

    fun <T> runIfAllowed(block: () -> T): T? {
        if (!active.compareAndSet(expected = true, new = false)) return null
        try {
            return block()
        } finally {
            active.set(true)
        }
    }

    suspend fun <T> runIfAllowedSuspend(block: suspend () -> T): T? {
        if (!active.compareAndSet(expected = true, new = false)) return null
        try {
            return block()
        } finally {
            active.set(true)
        }
    }
}
