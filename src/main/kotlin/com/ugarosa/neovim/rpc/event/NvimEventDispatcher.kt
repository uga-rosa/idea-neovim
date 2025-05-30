package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.connection.NvimConnectionManager
import com.ugarosa.neovim.rpc.transport.NvimObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

typealias NotificationHandler = suspend (List<NvimObject>) -> Unit

class NeovimEventDispatcher(
    connectionManager: NvimConnectionManager,
    scope: CoroutineScope,
) {
    private val logger = myLogger()

    private val handlers = mutableMapOf<String, MutableList<NotificationHandler>>()

    init {
        connectionManager.notificationFlow
            .onEach { notification ->
                handlers[notification.method]?.forEach { handler ->
                    try {
                        handler(notification.params)
                    } catch (e: Exception) {
                        logger.warn("Error in handler for ${notification.method}", e)
                    }
                }
            }
            .launchIn(scope)
    }

    fun register(
        method: String,
        handler: NotificationHandler,
    ) {
        handlers.getOrPut(method) { mutableListOf() }
            .add(handler)
    }

    fun unregister(
        method: String,
        handler: NotificationHandler,
    ) {
        handlers[method]?.remove(handler)
    }

    fun clear() {
        handlers.clear()
    }
}
