package com.ugarosa.neovim.config.nvim.option

@JvmInline
value class Filetype(val value: String) {
    companion object : RawOptionParser<Filetype> {
        override fun fromRaw(raw: Any): Filetype {
            val stringValue = raw.toString()
            return Filetype(stringValue)
        }

        override val default = Filetype("")
    }
}
