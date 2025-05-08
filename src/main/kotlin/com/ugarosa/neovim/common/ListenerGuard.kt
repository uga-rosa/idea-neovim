package com.ugarosa.neovim.common

import java.util.concurrent.atomic.AtomicBoolean

class ListenerGuard<T : Any>(
    private val listener: T,
    private val add: (T) -> Unit,
    private val remove: (T) -> Unit,
) {
    private var isRegistered = AtomicBoolean(false)

    fun unregister() {
        if (isRegistered.compareAndSet(true, false)) {
            remove(listener)
        }
    }

    fun register() {
        if (isRegistered.compareAndSet(false, true)) {
            add(listener)
        }
    }

    fun <R> runWithoutListener(block: () -> R): R? {
        unregister()
        try {
            return block()
        } finally {
            register()
        }
    }

    suspend fun <R> runWithoutListenerSuspend(block: suspend () -> R): R? {
        unregister()
        try {
            return block()
        } finally {
            register()
        }
    }
}
