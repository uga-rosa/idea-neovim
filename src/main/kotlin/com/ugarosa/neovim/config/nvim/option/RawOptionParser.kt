package com.ugarosa.neovim.config.nvim.option

interface RawOptionParser<T> {
    fun fromRaw(raw: Any): T

    val default: T
}
