package com.ugarosa.neovim.mode

import com.ugarosa.neovim.config.neovim.option.Selection

data class NeovimMode(
    val kind: NeovimModeKind,
) {
    companion object {
        // The ui `mode_change` event sends the mode string, but it's not same as the return value of mode().
        // For example, this could be "operator", "visual", "replace" or "cmdline_normal".
        fun fromModeChangeEvent(raw: String): NeovimMode {
            val kind =
                when (raw[0]) {
                    'n', 'o' -> NeovimModeKind.NORMAL
                    'v' -> NeovimModeKind.VISUAL
                    'i' -> NeovimModeKind.INSERT
                    'r' -> NeovimModeKind.REPLACE
                    'c' -> NeovimModeKind.COMMAND
                    else -> NeovimModeKind.OTHER
                }
            return NeovimMode(kind)
        }

        // The ui `mode_change` event does not send selection mode.
        // So I set autocmd for changing select mode.
        fun fromMode(raw: String): NeovimMode {
            val kind =
                when (raw[0]) {
                    's', 'S', '\u0013' -> NeovimModeKind.SELECT
                    else -> NeovimModeKind.OTHER
                }
            return NeovimMode(kind)
        }

        val default = NeovimMode(NeovimModeKind.NORMAL)
    }

    fun isBlock(selection: Selection): Boolean =
        when (kind) {
            NeovimModeKind.NORMAL -> true

            NeovimModeKind.SELECT,
            NeovimModeKind.VISUAL,
            -> selection == Selection.INCLUSIVE

            else -> false
        }

    fun isInsert(): Boolean = kind == NeovimModeKind.INSERT

    fun isVisualOrSelect(): Boolean = kind == NeovimModeKind.VISUAL || kind == NeovimModeKind.SELECT

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
