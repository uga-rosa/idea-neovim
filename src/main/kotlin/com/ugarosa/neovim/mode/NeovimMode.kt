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
                    'v' -> NeovimModeKind.VISUAL
                    'V' -> NeovimModeKind.VISUAL_LINE
                    '\u0016' -> NeovimModeKind.VISUAL_BLOCK // Ctrl-V
                    's' -> NeovimModeKind.SELECT
                    'S' -> NeovimModeKind.SELECT_LINE
                    '\u0013' -> NeovimModeKind.SELECT_BLOCK // Ctrl-S
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
            NeovimModeKind.VISUAL_LINE,
            NeovimModeKind.VISUAL_BLOCK,
            NeovimModeKind.SELECT,
            NeovimModeKind.SELECT_LINE,
            NeovimModeKind.SELECT_BLOCK,
            -> true

            else -> false
        }

    fun isInsert(): Boolean = kind == NeovimModeKind.INSERT
}

enum class NeovimModeKind {
    NORMAL,
    VISUAL,
    VISUAL_LINE,
    VISUAL_BLOCK,
    SELECT,
    SELECT_LINE,
    SELECT_BLOCK,
    INSERT,
    REPLACE,
    COMMAND,
    OTHER,
}
