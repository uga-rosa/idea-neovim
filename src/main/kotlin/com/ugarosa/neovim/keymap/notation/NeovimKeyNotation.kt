package com.ugarosa.neovim.keymap.notation

import java.awt.event.KeyEvent

enum class NeovimKeyModifier(val neovimPrefix: String) {
    CTRL("C"),
    SHIFT("S"),
    ALT("A"),
    META("M"),
}

data class NeovimKeyNotation(
    val modifiers: List<NeovimKeyModifier>,
    val key: String,
) {
    companion object {
        private val keyCodeToNeovim: Map<Int, String> =
            supportedKeys.associate { it.awtKeyCode to it.neovimName }

        fun fromKeyEvent(event: KeyEvent): NeovimKeyNotation? {
            // Collect modifiers
            val mods =
                mutableListOf<NeovimKeyModifier>().apply {
                    if (event.isControlDown) add(NeovimKeyModifier.CTRL)
                    if (event.isShiftDown) add(NeovimKeyModifier.SHIFT)
                    if (event.isAltDown) add(NeovimKeyModifier.ALT)
                    if (event.isMetaDown) add(NeovimKeyModifier.META)
                }

            // Fallback: printable char (%, ~, etc.)
            val charFallback =
                event.keyChar.takeIf {
                    it != KeyEvent.CHAR_UNDEFINED && !it.isISOControl()
                }?.toString()

            val mapping =
                keyCodeToNeovim[event.keyCode]
                    ?: charFallback
                    ?: return null

            return NeovimKeyNotation(modifiers = mods, key = mapping)
        }
    }

    /**
     * Render as Neovim notation string.
     * Examples: "<C-A-x>", "g", "<Esc>"
     */
    override fun toString(): String {
        return when {
            modifiers.isNotEmpty() ->
                "<${modifiers.joinToString("-") { it.neovimPrefix }}-$key>"

            key.length == 1 ->
                key.lowercase()

            else ->
                "<$key>"
        }
    }
}
