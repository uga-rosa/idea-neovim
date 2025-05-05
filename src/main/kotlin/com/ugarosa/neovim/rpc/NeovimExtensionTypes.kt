package com.ugarosa.neovim.rpc

import org.msgpack.core.MessagePack
import org.msgpack.value.Value

// :h api-definitions
// Special types (msgpack EXT)
data class BufferId(val value: Int)

data class WindowId(val value: Int)

data class TabPageId(val value: Int)

fun Value.asBufferId(): BufferId = toTypedId(0, ::BufferId)

fun Value.asWindowId(): WindowId = toTypedId(1, ::WindowId)

fun Value.asTabPageId(): TabPageId = toTypedId(2, ::TabPageId)

private inline fun <A> Value.toTypedId(
    expectedType: Byte,
    constructor: (Int) -> A,
): A {
    require(isExtensionValue) { "Must be extension type" }
    val ext = asExtensionValue()
    require(ext.type == expectedType) { "Expected type $expectedType, but got ${ext.type}" }
    val value = ext.data.toIntFromMsgpack()
    return constructor(value)
}

private fun ByteArray.toIntFromMsgpack(): Int {
    return MessagePack.newDefaultUnpacker(this).unpackInt()
}
