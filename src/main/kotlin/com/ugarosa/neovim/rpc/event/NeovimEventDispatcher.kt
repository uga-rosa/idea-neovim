package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.connection.NeovimConnectionManager
import com.ugarosa.neovim.rpc.transport.NeovimObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

typealias NotificationHandler = suspend (List<NeovimObject>) -> Unit

class NeovimEventDispatcher(
    connectionManager: NeovimConnectionManager,
    private val scope: CoroutineScope,
) {
    private val logger = myLogger()

    private val handlers = mutableMapOf<String, MutableList<NotificationHandler>>()

    init {
        connectionManager.notificationFlow
            .onEach { notification ->
                handlers[notification.method]?.forEach { handler ->
                    scope.launch {
                        try {
                            handler(notification.params)
                        } catch (e: Exception) {
                            logger.warn("Error in handler for ${notification.method}", e)
                        }
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
