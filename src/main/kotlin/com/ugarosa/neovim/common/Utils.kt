package com.ugarosa.neovim.common

import org.msgpack.value.MapValue
import org.msgpack.value.Value
import java.awt.event.KeyEvent

fun utf8ByteOffsetToCharOffset(
    text: String,
    byteOffset: Int,
): Int {
    var bytes = 0
    var index = 0
    for (ch in text) {
        val utf8Bytes = ch.toString().toByteArray(Charsets.UTF_8)
        bytes += utf8Bytes.size
        if (bytes > byteOffset) {
            break
        }
        index++
    }
    return index
}

fun neovimNotation(e: KeyEvent): String {
    when (e.keyCode) {
        KeyEvent.VK_CONTROL,
        KeyEvent.VK_SHIFT,
        KeyEvent.VK_ALT,
        KeyEvent.VK_META,
        -> return ""
    }

    val modifiers =
        buildList {
            if (e.isControlDown) add("C")
            if (e.isShiftDown) add("S")
            if (e.isAltDown) add("A")
            if (e.isMetaDown) add("M")
        }
    val key = KeyEvent.getKeyText(e.keyCode)

    return if (modifiers.isNotEmpty()) {
        "<${modifiers.joinToString("-")}-$key>"
    } else if (key.length == 1) {
        key.lowercase()
    } else {
        "<$key>"
    }
}

fun MapValue.get(key: String): Value? {
    return this.map().entries
        .firstOrNull { it.key.isStringValue && it.key.asStringValue().asString() == key }
        ?.value
}
