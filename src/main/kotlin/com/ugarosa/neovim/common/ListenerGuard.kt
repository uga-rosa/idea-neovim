package com.ugarosa.neovim.common

import arrow.atomic.AtomicBoolean
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.cursor.NeovimCaretListener

val CARET_LISTENER_GUARD_KEY = Key.create<ListenerGuard<NeovimCaretListener>>("CARET_LISTENER_GUARD_KEY")

class ListenerGuard<T : Any>(
    private val listener: T,
    private val add: (T) -> Unit,
    private val remove: (T) -> Unit,
) {
    private var isRegistered = AtomicBoolean(true)

    init {
        add(listener)
    }

    fun unregister() {
        if (isRegistered.compareAndSet(expected = true, new = false)) {
            remove(listener)
        }
    }

    fun register() {
        if (isRegistered.compareAndSet(expected = false, new = true)) {
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
