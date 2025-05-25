package com.ugarosa.neovim.config.nvim.option

enum class Selection(val value: String) {
    INCLUSIVE("inclusive"),
    EXCLUSIVE("exclusive"),
    OLD("old"),
    ;

    companion object : RawOptionParser<Selection> {
        override fun fromRaw(raw: Any): Selection {
            val stringValue = raw.toString()
            return entries.firstOrNull { it.value == stringValue }
                ?: throw IllegalArgumentException("Invalid value for Selection: $raw")
        }

        override val default = INCLUSIVE
    }
}
