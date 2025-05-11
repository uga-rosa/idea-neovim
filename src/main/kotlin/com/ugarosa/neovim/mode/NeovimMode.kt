package com.ugarosa.neovim.mode

import com.ugarosa.neovim.config.neovim.option.Selection

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

    fun isBlock(selection: Selection): Boolean =
        when (kind) {
            NeovimModeKind.NORMAL,
            NeovimModeKind.SELECT,
            -> true

            NeovimModeKind.VISUAL -> selection == Selection.INCLUSIVE

            else -> false
        }

    fun isInsert(): Boolean = kind == NeovimModeKind.INSERT

    fun isVisual(): Boolean = kind == NeovimModeKind.VISUAL

    fun isCommand(): Boolean = kind == NeovimModeKind.COMMAND
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
