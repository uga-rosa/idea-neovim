package com.ugarosa.neovim.config.neovim.option

interface RawOptionParser<T> {
    fun fromRaw(raw: Any): T

    val default: T
}
