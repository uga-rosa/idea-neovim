package com.ugarosa.neovim.rpc

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.rd.util.concurrentMapOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker
import org.msgpack.value.Value
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.APP)
class NeovimRpcClient(
    scope: CoroutineScope,
) {
    private val logger = thisLogger()
    private val messageIdGenerator = AtomicInteger(0)
    private val packer: MessagePacker
    private val unpacker: MessageUnpacker
    private val waitingResponses = concurrentMapOf<Int, CompletableDeferred<Response>>()

    private val pushHandlers = mutableListOf<suspend (PushNotification) -> Unit>()

    fun registerPushHandler(handler: suspend (PushNotification) -> Unit) {
        pushHandlers.add(handler)
    }

    init {
        val processManager = NeovimProcessManager()
        packer = MessagePack.newDefaultPacker(processManager.getOutputStream())
        unpacker = MessagePack.newDefaultUnpacker(processManager.getInputStream())

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                unpacker.unpackArrayHeader()
                when (val type = unpacker.unpackInt()) {
                    1 -> {
                        val msgId = unpacker.unpackInt()
                        val error = unpacker.unpackValue()
                        val result = unpacker.unpackValue()
                        logger.debug("Received response: $msgId, result: $result")
                        waitingResponses.remove(msgId)?.complete(Response(msgId, error, result))
                    }

                    2 -> {
                        val method = unpacker.unpackString()
                        val params = unpacker.unpackValue()
                        logger.debug("Received push notification: $method, params: $params")
                        withContext(Dispatchers.Default) {
                            val notification = PushNotification(method, params)
                            pushHandlers.forEach {
                                try {
                                    it(notification)
                                } catch (e: Exception) {
                                    logger.error("NeovimRpcClient: Unpacking loop error", e)
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
    }

    @Throws(TimeoutCancellationException::class, IllegalArgumentException::class)
    suspend fun requestAsync(
        method: String,
        params: List<Any?> = emptyList(),
        timeoutMills: Long? = 500,
    ): Response {
        val msgId = messageIdGenerator.getAndIncrement()
        val deferred = CompletableDeferred<Response>()
        waitingResponses[msgId] = deferred

        withContext(Dispatchers.IO) {
            synchronized(packer) {
                packer.packArrayHeader(4)
                packer.packInt(0) // 0 = Request
                packer.packInt(msgId)
                packer.packString(method)
                packParams(params)
                packer.flush()
            }
        }

        logger.debug("Sending request: $msgId, method: $method, params: $params")

        return if (timeoutMills == null) {
            deferred.await()
        } else {
            withTimeout(timeoutMills) {
                deferred.await()
            }
        }
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

            is BufferId -> packer.packLong(param.value)
            is WindowId -> packer.packLong(param.value)
            is TabPageId -> packer.packLong(param.value)

            else -> throw IllegalArgumentException("Unsupported param type: ${param::class}")
        }
    }

    private fun packParams(params: List<Any?>) {
        packer.packArrayHeader(params.size)
        params.forEach { packParam(it) }
    }

    data class Response(
        val msgId: Int,
        val error: Value,
        val result: Value,
    )

    data class PushNotification(
        val method: String,
        val params: Value,
    )
}
