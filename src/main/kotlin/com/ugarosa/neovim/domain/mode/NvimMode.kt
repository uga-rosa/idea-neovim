package com.ugarosa.neovim.domain.mode

import com.ugarosa.neovim.config.nvim.option.Selection

data class NvimMode(
    val kind: NvimModeKind,
) {
    companion object {
        // The ui `mode_change` event sends the mode string, but it's not same as the return value of mode().
        // For example, this could be "operator", "visual", "replace" or "cmdline_normal".
        fun fromModeChangeEvent(raw: String): NvimMode? {
            val kind =
                when (raw[0]) {
                    'n', 'o' -> NvimModeKind.NORMAL
                    'v' -> NvimModeKind.VISUAL
                    'i' -> NvimModeKind.INSERT
                    'r' -> NvimModeKind.REPLACE
                    'c' -> NvimModeKind.COMMAND
                    else -> null
                }
            return kind?.let { NvimMode(it) }
        }

        // The ui `mode_change` event does not send selection mode.
        // So I set autocmd for changing select mode.
        fun fromMode(raw: String): NvimMode? {
            val kind =
                when (raw[0]) {
                    'n' -> NvimModeKind.NORMAL
                    'v', 'V', '\u0016' -> NvimModeKind.VISUAL
                    's', 'S', '\u0013' -> NvimModeKind.SELECT
                    'i' -> NvimModeKind.INSERT
                    'R' -> NvimModeKind.REPLACE
                    'c' -> NvimModeKind.COMMAND
                    else -> null
                }
            return kind?.let { NvimMode(it) }
        }

        val default = NvimMode(NvimModeKind.NORMAL)
    }

    fun isBlock(selection: Selection): Boolean =
        when (kind) {
            NvimModeKind.NORMAL -> true

            NvimModeKind.SELECT,
            NvimModeKind.VISUAL,
            -> selection == Selection.INCLUSIVE

            else -> false
        }

    fun isInsert(): Boolean = kind == NvimModeKind.INSERT

    fun isVisualOrSelect(): Boolean = kind == NvimModeKind.VISUAL || kind == NvimModeKind.SELECT

    fun isCommand(): Boolean = kind == NvimModeKind.COMMAND
}

enum class NvimModeKind {
    NORMAL,
    VISUAL,
    SELECT,
    INSERT,
    REPLACE,
    COMMAND,
}
