package com.ugarosa.neovim.keymap.notation

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.logger.myLogger
import java.awt.event.KeyEvent

enum class NeovimKeyModifier(val neovimPrefix: String) {
    CTRL("C"),
    SHIFT("S"),
    ALT("A"),
    META("M"),
    ;

    companion object {
        private val map = entries.associateBy { it.neovimPrefix }

        fun fromString(prefix: String): NeovimKeyModifier? = map[prefix]
    }
}

data class NeovimKeyNotation(
    @XCollection(
        propertyElementName = "modifiers",
        elementName = "modifier",
    )
    val modifiers: List<NeovimKeyModifier>,
    @Attribute
    val key: String,
) {
    @Suppress("unused")
    constructor() : this(emptyList(), "")

    companion object {
        private val logger = myLogger()
        private val keyCodeToNeovim: Map<Int, String> =
            supportedKeys.associate { it.awtKeyCode to it.neovimName }

        fun fromKeyPressedEvent(event: KeyEvent): NeovimKeyNotation? {
            check(event.id == KeyEvent.KEY_PRESSED) { "Not a KEY_PRESSED event: $event" }

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
            check(event.id == KeyEvent.KEY_TYPED) { "Not a KEY_TYPED event: $event" }

            val c = event.keyChar
            if (c == KeyEvent.CHAR_UNDEFINED || c.isISOControl()) {
                logger.trace("Not a printable character: $event")
                return null
            }
            val key =
                when (val s = c.toString()) {
                    "<" -> "<lt>"
                    else -> s
                }
            return NeovimKeyNotation(emptyList(), key)
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

        // This regex will match:
        //   1. <Xxx>(Yyy)
        //   2. <Xxx>
        //   3. Any single character
        private val regex = Regex("""<[^>]+>\([^)]+\)|<[^>]+>|.""")

        fun parseNotations(notations: String): List<NeovimKeyNotation> {
            return regex.findAll(notations)
                .mapNotNull { mr -> parseSingleNotation(mr.value) }
                .toList()
        }

        private fun parseSingleNotation(notation: String): NeovimKeyNotation? {
            val text = notation.trim()
            // <Xxx> may have modifiers like <C-A-x>
            if (text.startsWith("<") && text.endsWith(">")) {
                // Special handing for <Nop>
                if (text.equals("<Nop>", ignoreCase = true)) {
                    return NeovimKeyNotation(emptyList(), "Nop")
                }

                val inner = text.substring(1, text.length - 1)
                val parts = inner.split("-")

                val rawKey = parts.last()
                val key =
                    supportedKeys.firstOrNull { it.neovimName.equals(rawKey, ignoreCase = true) }
                        ?.neovimName
                        ?: run {
                            logger.warn("Unknown key: $rawKey")
                            return null
                        }

                val mods =
                    parts.dropLast(1).map { token ->
                        NeovimKeyModifier.fromString(token)
                            ?: run {
                                logger.warn("Unknown modifier: $token")
                                return null
                            }
                    }
                return NeovimKeyNotation(mods, key)
            } else {
                // <Xxx>(Yyy) or any single character
                return NeovimKeyNotation(emptyList(), text)
            }
        }
    }

    fun toPrintableChar(): Char? {
        if (modifiers.isNotEmpty()) {
            logger.debug("Cannot convert to printable char: $this")
            return null
        }
        if (key.length != 1) {
            logger.debug("Cannot convert to printable char: $this")
            return null
        }
        return key[0]
    }

    /**
     * Render as Neovim notation string.
     * Examples: "<C-A-x>", "g", "<Esc>", "<Plug>(FooBar)"
     */
    override fun toString(): String {
        return when {
            modifiers.isNotEmpty() -> "<${modifiers.joinToString("-") { it.neovimPrefix }}-$key>"

            key.length == 1 -> key

            key.startsWith("<") -> key

            else -> "<$key>"
        }
    }
}
