package com.ugarosa.neovim.rpc.transport

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.process.NeovimProcessManager
import com.ugarosa.neovim.rpc.type.TabpageId
import com.ugarosa.neovim.rpc.type.WindowId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.msgpack.core.MessagePack

private const val REQUEST_TYPE = 0
private const val RESPONSE_TYPE = 1
private const val NOTIFICATION_TYPE = 2

class NeovimTransport(
    private val processManager: NeovimProcessManager = NeovimProcessManager(),
) {
    private val packer = MessagePack.newDefaultPacker(processManager.getOutputStream())
    private val unpacker = MessagePack.newDefaultUnpacker(processManager.getInputStream())

    suspend fun sendRequest(
        id: Int,
        method: String,
        params: List<Any?>,
    ) = withContext(Dispatchers.IO) {
        // [type, msgId, method, params]
        packer.packArrayHeader(4)
        packer.packInt(REQUEST_TYPE)
        packer.packInt(id)
        packer.packString(method)
        packParams(params)
        packer.flush()
    }

    suspend fun sendNotification(
        method: String,
        params: List<Any?>,
    ) = withContext(Dispatchers.IO) {
        // [type, method, params]
        packer.packArrayHeader(3)
        packer.packInt(NOTIFICATION_TYPE)
        packer.packString(method)
        packParams(params)
        packer.flush()
    }

    suspend fun receive(): RpcMessage =
        withContext(Dispatchers.IO) {
            unpacker.unpackArrayHeader()
            when (unpacker.unpackInt()) {
                RESPONSE_TYPE -> {
                    // [type, msgId, error, result]
                    val id = unpacker.unpackInt()
                    val error = unpacker.unpackValue().asNeovimObject()
                    val result = unpacker.unpackValue().asNeovimObject()
                    RpcMessage.Response(id, result, error)
                }

                NOTIFICATION_TYPE -> {
                    val method = unpacker.unpackString()
                    val params = unpacker.unpackValue().asArrayValue().list().map { it.asNeovimObject() }
                    RpcMessage.Notification(method, params)
                }

                else -> error("Unknown message type")
            }
        }

    fun close() {
        processManager.close()
        packer.close()
        unpacker.close()
    }

    private fun packParams(params: List<Any?>) {
        packer.packArrayHeader(params.size)
        params.forEach { packParam(it) }
    }

    private fun packParam(param: Any?) {
        when (param) {
            is String -> packer.packString(param)
            is Int -> packer.packInt(param)
            is Long -> packer.packLong(param)
            is Float -> packer.packFloat(param)
            is Double -> packer.packDouble(param)
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

            is BufferId -> packer.packLong(param.id)
            is WindowId -> packer.packLong(param.id)
            is TabpageId -> packer.packLong(param.id)

            else -> error("Unsupported param type: ${param::class}")
        }
    }
}
