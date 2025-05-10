package com.ugarosa.neovim.rpc.client

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.TabPageId
import com.ugarosa.neovim.rpc.WindowId
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.process.AutoNeovimProcessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import org.msgpack.core.MessagePack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.APP)
class NeovimRpcClientImpl(
    override val scope: CoroutineScope,
) : NeovimRpcClient {
    private val logger = thisLogger()
    private val processManager = AutoNeovimProcessManager()
    private val healthCheck = CompletableDeferred<Unit>()
    private val packer = MessagePack.newDefaultPacker(processManager.getOutputStream())
    private val unpacker = MessagePack.newDefaultUnpacker(processManager.getInputStream())
    private val messageIdGenerator = AtomicInteger(0)
    private val sendMutex = Mutex()
    private val waitingResponses = ConcurrentHashMap<Int, CompletableDeferred<NeovimRpcClient.Response>>()

    private val pushHandlers = CopyOnWriteArrayList<suspend (NeovimRpcClient.PushNotification) -> Unit>()

    override fun registerPushHandler(handler: suspend (NeovimRpcClient.PushNotification) -> Unit) {
        pushHandlers.add(handler)
    }

    init {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                unpacker.unpackArrayHeader()
                when (val type = unpacker.unpackInt()) {
                    1 -> {
                        val msgId = unpacker.unpackInt()
                        val error = unpacker.unpackValue()
                        val result = unpacker.unpackValue()
                        val response = NeovimRpcClient.Response(msgId, error, result)
                        logger.trace("Received response [$msgId]: $response")
                        waitingResponses.remove(msgId)?.complete(response)
                    }

                    2 -> {
                        val method = unpacker.unpackString()
                        val params = unpacker.unpackValue()
                        val push = NeovimRpcClient.PushNotification(method, params)
                        logger.trace("Received push notification: $push")
                        withContext(Dispatchers.Default) {
                            pushHandlers.forEach {
                                try {
                                    it(push)
                                } catch (e: Exception) {
                                    logger.warn("Error in push handler: $it", e)
                                }
                            }
                        }
                    }

                    else -> {
                        logger.error("Unknown message type: $type")
                    }
                }
            }
        }

        scope.launch {
            try {
                requestInternal("nvim_get_api_info", emptyList(), 5000)
                    ?.let {
                        val chanId = it.result.asArrayValue().list()[0].asIntegerValue().toInt()
                        ChanIdManager.set(chanId)

                        logger.info("Connected to Neovim: $it")
                        healthCheck.complete(Unit)
                    } ?: throw IllegalStateException("Failed to connect to Neovim")
            } catch (e: Exception) {
                healthCheck.completeExceptionally(e)
            }
        }
    }

    override suspend fun request(
        method: String,
        params: List<Any?>,
        timeoutMills: Long?,
    ): NeovimRpcClient.Response? {
        healthCheck.await()

        return requestInternal(method, params, timeoutMills)
    }

    private suspend fun requestInternal(
        method: String,
        params: List<Any?>,
        timeoutMills: Long?,
    ): NeovimRpcClient.Response? {
        val msgId = messageIdGenerator.getAndIncrement()
        val deferred = CompletableDeferred<NeovimRpcClient.Response>()
        waitingResponses[msgId] = deferred

        logger.trace("Sending request [$msgId]: method: $method, params: $params")

        try {
            withContext(Dispatchers.IO) {
                sendMutex.withLock {
                    packer.packArrayHeader(4)
                    packer.packInt(0) // 0 = Request
                    packer.packInt(msgId)
                    packer.packString(method)
                    packParams(params)
                    packer.flush()
                }
            }

            return if (timeoutMills == null) {
                deferred.await()
            } else {
                withTimeout(timeoutMills) { deferred.await() }
            }
        } catch (e: Exception) {
            handleException(e, msgId)
            return null
        } finally {
            waitingResponses.remove(msgId)
        }
    }

    override suspend fun notify(
        method: String,
        params: List<Any?>,
    ) {
        healthCheck.await()

        logger.trace("Sending notification: method: $method, params: $params")

        try {
            withContext(Dispatchers.IO) {
                sendMutex.withLock {
                    packer.packArrayHeader(3)
                    packer.packInt(2) // 2 = Notification
                    packer.packString(method)
                    packParams(params)
                    packer.flush()
                }
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(
        e: Exception,
        msgId: Int? = null,
    ) {
        waitingResponses.remove(msgId)?.cancel()
        when (e) {
            is TimeoutCancellationException -> logger.warn("Request timed out: $msgId")
            is CancellationException -> throw e
            is IOException -> {
                waitingResponses.values.forEach { it.cancel() }
                waitingResponses.clear()
                processManager.close()
                logger.error("IO exception. Closed the connection.", e)
            }

            else -> logger.warn("Exception", e)
        }
    }

    private fun packParams(params: List<Any?>) {
        packer.packArrayHeader(params.size)
        params.forEach { packParam(it) }
    }

    private fun packParam(param: Any?) {
        when (param) {
            is String -> packer.packString(param)
            is Int -> packer.packInt(param)
            is Boolean -> packer.packBoolean(param)
            is List<*> -> {
                packer.packArrayHeader(param.size)
                param.forEach { packParam(it) }
            }

            is Map<*, *> -> {
                packer.packMapHeader(param.size)
                param.forEach { (key, value) ->
                    packParam(key)
                    packParam(value)
                }
            }

            null -> packer.packNil()

            is BufferId -> packer.packInt(param.value)
            is WindowId -> packer.packInt(param.value)
            is TabPageId -> packer.packInt(param.value)

            else -> throw IllegalArgumentException("Unsupported param type: ${param::class}")
        }
    }
}
