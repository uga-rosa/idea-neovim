package com.ugarosa.neovim.keymap.notation

import com.intellij.openapi.diagnostic.thisLogger
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
        private val logger = thisLogger()
        private val keyCodeToNeovim: Map<Int, String> =
            supportedKeys.associate { it.awtKeyCode to it.neovimName }

        fun fromKeyPressedEvent(event: KeyEvent): NeovimKeyNotation? {
            if (event.id != KeyEvent.KEY_PRESSED) {
                logger.warn("Not a KEY_PRESSED event: $event")
                return null
            }

            val modifiers =
                mutableListOf<NeovimKeyModifier>().apply {
                    if (event.isControlDown) add(NeovimKeyModifier.CTRL)
                    if (event.isShiftDown) add(NeovimKeyModifier.SHIFT)
                    if (event.isAltDown) add(NeovimKeyModifier.ALT)
                    if (event.isMetaDown) add(NeovimKeyModifier.META)
                }
            val key =
                keyCodeToNeovim[event.keyCode]
                    ?: run {
                        logger.trace("Not a supported key event: $event")
                        return null
                    }
            return NeovimKeyNotation(modifiers, key)
        }

        fun fromKeyTypedEvent(event: KeyEvent): NeovimKeyNotation? {
            if (event.id == KeyEvent.KEY_PRESSED) {
                logger.warn("Not a KEY_TYPED event: $event")
                return null
            }
            val c = event.keyChar
            if (c == KeyEvent.CHAR_UNDEFINED || c.isISOControl()) {
                logger.trace("Not a printable character: $event")
                return null
            }
            return NeovimKeyNotation(emptyList(), c.toString())
        }

        fun fromModsAndKey(
            mods: Int,
            key: String,
        ): NeovimKeyNotation {
            val modifiers =
                buildList {
                    if (mods and KeyEvent.CTRL_DOWN_MASK != 0) add(NeovimKeyModifier.CTRL)
                    if (mods and KeyEvent.SHIFT_DOWN_MASK != 0) add(NeovimKeyModifier.SHIFT)
                    if (mods and KeyEvent.ALT_DOWN_MASK != 0) add(NeovimKeyModifier.ALT)
                    if (mods and KeyEvent.META_DOWN_MASK != 0) add(NeovimKeyModifier.META)
                }
            return NeovimKeyNotation(modifiers, key)
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
