package com.ugarosa.neovim.common

import java.util.concurrent.atomic.AtomicInteger

object GroupIdGenerator {
    private val counter = AtomicInteger()

    fun generate(prefix: String = "ApplyBufLinesEvent"): String {
        return "$prefix-${counter.getAndIncrement()}"
    }
}
