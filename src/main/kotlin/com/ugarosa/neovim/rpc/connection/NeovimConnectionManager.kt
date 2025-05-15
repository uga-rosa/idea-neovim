package com.ugarosa.neovim.rpc.connection

import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.transport.NeovimObject
import com.ugarosa.neovim.rpc.transport.NeovimTransport
import com.ugarosa.neovim.rpc.transport.RpcMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NeovimConnectionManager(
    private val transport: NeovimTransport,
    scope: CoroutineScope,
    private val timeout: Duration = 3.seconds,
) {
    private val logger = myLogger()
    private val msgIdGen = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<RpcMessage.Response>>()
    private val sendMutex = Mutex()
    private val _notificationFlow = MutableSharedFlow<RpcMessage.Notification>(extraBufferCapacity = Int.MAX_VALUE)

    val notificationFlow: SharedFlow<RpcMessage.Notification> = _notificationFlow

    init {
        scope.launch(Dispatchers.IO + SupervisorJob()) {
            try {
                while (isActive) {
                    when (val msg = transport.receive()) {
                        is RpcMessage.Response -> {
                            logger.trace("Received response: $msg")
                            pending.remove(msg.id)?.complete(msg)
                        }

                        is RpcMessage.Notification -> {
                            logger.trace("Received notification: $msg")
                            _notificationFlow.emit(msg)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                transport.close()
                pending.values.forEach { it.cancel() }
                pending.clear()
            }
        }
    }

    suspend fun request(
        method: String,
        params: List<Any?>,
    ): NeovimObject {
        logger.trace("Request: $method, params: $params")
        val id = msgIdGen.getAndIncrement()
        val deferred = CompletableDeferred<RpcMessage.Response>()
        pending[id] = deferred

        sendMutex.withLock {
            transport.sendRequest(id, method, params)
        }

        val resp =
            withTimeout(timeout) {
                deferred.await()
            }
        check(resp.error is NeovimObject.Nil) { "Error: ${resp.error}" }
        return resp.result
    }

    suspend fun notify(
        method: String,
        params: List<Any?>,
    ) {
        logger.trace("Notify: $method, params: $params")
        sendMutex.withLock {
            transport.sendNotification(method, params)
        }
    }

    fun close() {
        transport.close()
    }
}
