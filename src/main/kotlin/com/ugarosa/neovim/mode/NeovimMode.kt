package com.ugarosa.neovim.mode

data class NeovimMode(
    val kind: NeovimModeKind,
    val raw: String,
) {
    companion object {
        fun fromRaw(raw: String): NeovimMode {
            val kind =
                when (raw[0]) {
                    'n' -> NeovimModeKind.NORMAL
                    'v', 'V', '\u0016' -> NeovimModeKind.VISUAL // Ctrl-V
                    's', 'S', '\u0013' -> NeovimModeKind.SELECT // Ctrl-S
                    'i' -> NeovimModeKind.INSERT
                    'R' -> NeovimModeKind.REPLACE
                    'c' -> NeovimModeKind.COMMAND
                    else -> NeovimModeKind.OTHER
                }
            return NeovimMode(kind, raw)
        }

        val default = NeovimMode(NeovimModeKind.NORMAL, "n")
    }

    fun isBlock(): Boolean =
        when (kind) {
            NeovimModeKind.NORMAL,
            NeovimModeKind.VISUAL,
            NeovimModeKind.SELECT,
            -> true

            else -> false
        }

    fun isInsert(): Boolean = kind == NeovimModeKind.INSERT
}

enum class NeovimModeKind {
    NORMAL,
    VISUAL,
    SELECT,
    INSERT,
    REPLACE,
    COMMAND,
    OTHER,
}
