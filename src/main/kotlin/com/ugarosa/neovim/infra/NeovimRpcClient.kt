package com.ugarosa.neovim.infra

import com.ugarosa.neovim.service.BufferId
import com.ugarosa.neovim.service.TabPageId
import com.ugarosa.neovim.service.WindowId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

class NeovimRpcClient(
    input: InputStream,
    output: OutputStream,
) {
    private val messageIdGenerator = AtomicInteger(0)
    private val packer = MessagePack.newDefaultPacker(output)
    private val unpacker = MessagePack.newDefaultUnpacker(input)
    private val responseChannel = Channel<Response>(Channel.UNLIMITED)

    private val pushHandlers = mutableListOf<(PushNotification) -> Unit>()

    fun registerPushHandler(handler: (PushNotification) -> Unit) {
        pushHandlers.add(handler)
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                unpacker.unpackArrayHeader()
                val type = unpacker.unpackInt()
                when (type) {
                    1 -> {
                        val msgId = unpacker.unpackInt()
                        val error = unpacker.unpackValue()
                        val result = unpacker.unpackValue()
                        responseChannel.send(Response(msgId, error, result))
                    }

                    2 -> {
                        val method = unpacker.unpackString()
                        val params = unpacker.unpackValue()
                        val notification = PushNotification(method, params)
                        pushHandlers.forEach { it(notification) }
                    }

                    else -> {
                        error("Unknown message type: $type")
                    }
                }
            }
        }
    }

    fun requestSync(
        method: String,
        params: List<Any?> = emptyList(),
        timeoutMillis: Int? = 500,
    ): Response {
        val msgId = messageIdGenerator.getAndIncrement()

        packer.packArrayHeader(4)
        packer.packInt(0) // 0 = Request
        packer.packInt(msgId)
        packer.packString(method)
        packParams(params)
        packer.flush()

        val deadline = timeoutMillis?.let { System.currentTimeMillis() + it }

        while (true) {
            val result = responseChannel.tryReceive()
            if (result.isSuccess) {
                val response = result.getOrThrow()
                if (response.msgId == msgId) {
                    return response
                } else {
                    // TODO: Handle unexpected message
                }
            }
            if (deadline != null && System.currentTimeMillis() > deadline) {
                error("Timeout waiting for response to $method")
            }
            Thread.sleep(1)
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
