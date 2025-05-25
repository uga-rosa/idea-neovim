package com.ugarosa.neovim.rpc.transport

import org.msgpack.core.MessagePack
import org.msgpack.value.Value

fun Value.asNeovimObject(): NvimObject =
    when {
        isNilValue -> NvimObject.Nil
        isBooleanValue -> NvimObject.Bool(asBooleanValue().boolean)
        isIntegerValue -> NvimObject.Int64(asIntegerValue().toLong())
        isFloatValue -> NvimObject.Float64(asFloatValue().toDouble())
        isStringValue -> NvimObject.Str(asStringValue().asString())
        isArrayValue -> NvimObject.Array(asArrayValue().list().map { it.asNeovimObject() })
        isMapValue ->
            NvimObject.Dict(
                asMapValue().map()
                    .mapKeys { it.key.asStringValue().asString() }
                    .mapValues { it.value.asNeovimObject() },
            )

        isExtensionValue -> {
            val ext = asExtensionValue()
            when (ext.type) {
                EXT_BUFFER -> NvimObject.Buffer(ext.data.toLong())
                EXT_WINDOW -> NvimObject.Window(ext.data.toLong())
                EXT_TABPAGE -> NvimObject.Tabpage(ext.data.toLong())
                else -> error("Unknown EXT type ${ext.type}")
            }
        }

        else -> error("Unsupported MsgPack value: $this")
    }

private const val EXT_BUFFER = 0.toByte()
private const val EXT_WINDOW = 1.toByte()
private const val EXT_TABPAGE = 2.toByte()

private fun ByteArray.toLong(): Long = MessagePack.newDefaultUnpacker(this).unpackLong()
