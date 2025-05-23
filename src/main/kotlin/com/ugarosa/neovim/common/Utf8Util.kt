package com.ugarosa.neovim.common

fun utf8ByteOffsetToCharOffset(
    text: String,
    byteOffset: Int,
): Int {
    var acc = 0
    text.forEachIndexed { i, c ->
        val bytes = c.toString().toByteArray(Charsets.UTF_8).size
        if (acc + bytes > byteOffset) return i
        acc += bytes
    }
    return text.length
}

fun charOffsetToUtf8ByteOffset(
    text: String,
    charOffset: Int,
): Int {
    val safeOffset = charOffset.coerceAtMost(text.length)
    return text.substring(0, safeOffset).toByteArray(Charsets.UTF_8).size
}

fun String.takeByte(byteCount: Int): String {
    val charCount = utf8ByteOffsetToCharOffset(this, byteCount)
    return this.take(charCount)
}
