package com.ugarosa.neovim.rpc.client

import arrow.core.Either
import arrow.core.raise.either
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.rd.util.concurrentMapOf
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.TabPageId
import com.ugarosa.neovim.rpc.WindowId
import com.ugarosa.neovim.rpc.process.AutoNeovimProcessManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.core.MessageUnpacker
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.APP)
class NeovimRpcClientImpl(
    scope: CoroutineScope,
) : NeovimRpcClient {
    private val logger = thisLogger()
    private val messageIdGenerator = AtomicInteger(0)
    private val packer: MessagePacker
    private val unpacker: MessageUnpacker
    private val waitingResponses = concurrentMapOf<Int, CompletableDeferred<NeovimRpcClient.Response>>()

    private val pushHandlers = mutableListOf<suspend (NeovimRpcClient.PushNotification) -> Unit>()

    override fun registerPushHandler(handler: suspend (NeovimRpcClient.PushNotification) -> Unit) {
        pushHandlers.add(handler)
    }

    init {
        val processManager = AutoNeovimProcessManager()
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
                        logger.trace("Received response: $msgId, result: $result")
                        waitingResponses.remove(msgId)?.complete(NeovimRpcClient.Response(msgId, error, result))
                    }

                    2 -> {
                        val method = unpacker.unpackString()
                        val params = unpacker.unpackValue()
                        logger.trace("Received push notification: $method, params: $params")
                        withContext(Dispatchers.Default) {
                            val notification = NeovimRpcClient.PushNotification(method, params)
                            pushHandlers.forEach { it(notification) }
                        }
                    }

                    else -> {
                        logger.warn("Unknown message type: $type")
                    }
                }
            }
        }
    }

    override suspend fun requestAsync(
        method: String,
        params: List<Any?>,
        timeoutMills: Long?,
    ): Either<NeovimRpcClient.RequestError, NeovimRpcClient.Response> =
        either {
            val msgId = messageIdGenerator.getAndIncrement()
            val deferred = CompletableDeferred<NeovimRpcClient.Response>()
            waitingResponses[msgId] = deferred

            try {
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
            } catch (_: IOException) {
                raise(NeovimRpcClient.RequestError.IO)
            } catch (_: IllegalStateException) {
                raise(NeovimRpcClient.RequestError.BadRequest)
            }

            logger.trace("Sending request: $msgId, method: $method, params: $params")

            if (timeoutMills == null) {
                deferred.await()
            } else {
                try {
                    withTimeout(timeoutMills) {
                        deferred.await()
                    }
                } catch (_: TimeoutCancellationException) {
                    waitingResponses.remove(msgId)
                    raise(NeovimRpcClient.RequestError.Timeout)
                }
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
