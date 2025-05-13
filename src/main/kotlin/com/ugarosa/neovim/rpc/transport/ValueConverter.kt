package com.ugarosa.neovim.rpc.transport

import org.msgpack.core.MessagePack
import org.msgpack.value.Value

fun Value.asNeovimObject(): NeovimObject =
    when {
        isNilValue -> NeovimObject.Nil
        isBooleanValue -> NeovimObject.Bool(asBooleanValue().boolean)
        isIntegerValue -> NeovimObject.Int64(asIntegerValue().toLong())
        isFloatValue -> NeovimObject.Float64(asFloatValue().toDouble())
        isStringValue -> NeovimObject.Str(asStringValue().asString())
        isArrayValue -> NeovimObject.Array(asArrayValue().list().map { it.asNeovimObject() })
        isMapValue ->
            NeovimObject.Dict(
                asMapValue().map()
                    .mapKeys { it.key.asStringValue().asString() }
                    .mapValues { it.value.asNeovimObject() },
            )

        isExtensionValue -> {
            val ext = asExtensionValue()
            when (ext.type) {
                EXT_BUFFER -> NeovimObject.Buffer(ext.data.toLong())
                EXT_WINDOW -> NeovimObject.Window(ext.data.toLong())
                EXT_TABPAGE -> NeovimObject.Tabpage(ext.data.toLong())
                else -> error("Unknown EXT type ${ext.type}")
            }
        }

        else -> error("Unsupported MsgPack value: $this")
    }

private const val EXT_BUFFER = 0.toByte()
private const val EXT_WINDOW = 1.toByte()
private const val EXT_TABPAGE = 2.toByte()

private fun ByteArray.toLong(): Long = MessagePack.newDefaultUnpacker(this).unpackLong()
