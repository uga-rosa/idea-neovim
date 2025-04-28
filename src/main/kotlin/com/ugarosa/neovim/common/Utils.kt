package com.ugarosa.neovim.common

fun utf8ByteOffsetToCharOffset(text: String, byteOffset: Int): Int {
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
