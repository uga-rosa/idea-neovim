package com.ugarosa.neovim.config.nvim.option

fun Scrolloff?.getOrElse(default: Scrolloff): Scrolloff =
    if (this == null || this.value < 0) {
        default
    } else {
        this
    }

@JvmInline
value class Scrolloff(val value: Int) {
    companion object : RawOptionParser<Scrolloff> {
        override fun fromRaw(raw: Any): Scrolloff {
            val intValue =
                (raw as? Number)?.toInt()
                    ?: throw IllegalArgumentException("Invalid value for Scrolloff: $raw")
            return Scrolloff(intValue)
        }

        override val default = Scrolloff(0)
    }
}

fun Sidescrolloff?.getOrElse(default: Sidescrolloff): Sidescrolloff =
    if (this == null || this.value < 0) {
        default
    } else {
        this
    }

@JvmInline
value class Sidescrolloff(val value: Int) {
    companion object : RawOptionParser<Sidescrolloff> {
        override fun fromRaw(raw: Any): Sidescrolloff {
            val intValue =
                (raw as? Number)?.toInt()
                    ?: throw IllegalArgumentException("Invalid value for Scrolloff: $raw")
            return Sidescrolloff(intValue)
        }

        override val default = Sidescrolloff(0)
    }
}
