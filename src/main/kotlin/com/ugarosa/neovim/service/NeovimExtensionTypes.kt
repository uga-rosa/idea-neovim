package com.ugarosa.neovim.service

import org.msgpack.core.MessagePack
import org.msgpack.value.Value

// :h api-definitions
// Special types (msgpack EXT)
data class BufferId(val value: Long)

data class WindowId(val value: Long)

data class TabPageId(val value: Long)

fun Value.asBufferId(): BufferId {
    require(this.isExtensionValue) { "Expected ExtensionValue, but got: $this" }
    val ext = this.asExtensionValue()
    require(ext.type == 0.toByte()) { "Expected Buffer (code=0), but got: ${ext.type}" }
    val value = ext.data.toLongFromMsgpack()
    return BufferId(value)
}

fun Value.asWindowId(): WindowId {
    require(this.isExtensionValue) { "Expected ExtensionValue, but got: $this" }
    val ext = this.asExtensionValue()
    require(ext.type == 1.toByte()) { "Expected Window (code=1), but got: ${ext.type}" }
    val value = ext.data.toLongFromMsgpack()
    return WindowId(value)
}

fun Value.asTabPageId(): TabPageId {
    require(this.isExtensionValue) { "Expected ExtensionValue, but got: $this" }
    val ext = this.asExtensionValue()
    require(ext.type == 2.toByte()) { "Expected TabPage (code=2), but got: ${ext.type}" }
    val value = ext.data.toLongFromMsgpack()
    return TabPageId(value)
}

private fun ByteArray.toLongFromMsgpack(): Long {
    return MessagePack.newDefaultUnpacker(this).unpackLong()
}
