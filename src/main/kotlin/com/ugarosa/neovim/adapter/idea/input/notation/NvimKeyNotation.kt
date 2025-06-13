package com.ugarosa.neovim.adapter.idea.input.notation

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import com.ugarosa.neovim.logger.myLogger
import java.awt.event.KeyEvent

enum class NvimKeyModifier(val nvimPrefix: String) {
    CTRL("C"),
    SHIFT("S"),
    ALT("A"),
    META("M"),
    ;

    companion object {
        private val map = entries.associateBy { it.nvimPrefix }

        fun fromString(prefix: String): NvimKeyModifier? = map[prefix]
    }
}

data class NvimKeyNotation(
    @XCollection(
        propertyElementName = "modifiers",
        elementName = "modifier",
    )
    val modifiers: List<NvimKeyModifier>,
    @Attribute
    val key: String,
) {
    @Suppress("unused")
    constructor() : this(emptyList(), "")

    companion object {
        private val logger = myLogger()
        private val keyCodeToNvim: Map<Int, String> =
            supportedKeys.associate { it.awtKeyCode to it.nvimName }

        fun fromKeyPressedEvent(event: KeyEvent): NvimKeyNotation? {
            check(event.id == KeyEvent.KEY_PRESSED) { "Not a KEY_PRESSED event: $event" }

            val modifiers =
                mutableListOf<NvimKeyModifier>().apply {
                    if (event.isControlDown) add(NvimKeyModifier.CTRL)
                    if (event.isShiftDown) add(NvimKeyModifier.SHIFT)
                    if (event.isAltDown) add(NvimKeyModifier.ALT)
                    if (event.isMetaDown) add(NvimKeyModifier.META)
                }
            val key =
                keyCodeToNvim[event.keyCode]
                    ?: run {
                        logger.trace("Not a supported key event: $event")
                        return null
                    }
            return NvimKeyNotation(modifiers, key)
        }

        fun fromKeyTypedEvent(event: KeyEvent): NvimKeyNotation? {
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
            return NvimKeyNotation(emptyList(), key)
        }

        fun fromModsAndKey(
            mods: Int,
            key: String,
        ): NvimKeyNotation {
            val modifiers =
                buildList {
                    if (mods and KeyEvent.CTRL_DOWN_MASK != 0) add(NvimKeyModifier.CTRL)
                    if (mods and KeyEvent.SHIFT_DOWN_MASK != 0) add(NvimKeyModifier.SHIFT)
                    if (mods and KeyEvent.ALT_DOWN_MASK != 0) add(NvimKeyModifier.ALT)
                    if (mods and KeyEvent.META_DOWN_MASK != 0) add(NvimKeyModifier.META)
                }
            return NvimKeyNotation(modifiers, key)
        }

        // This regex will match:
        //   1. <Xxx>(Yyy)
        //   2. <Xxx>
        //   3. Any single character
        private val regex = Regex("""<[^>]+>\([^)]+\)|<[^>]+>|.""")

        fun parseNotations(notations: String): List<NvimKeyNotation> {
            return regex.findAll(notations)
                .mapNotNull { mr -> parseSingleNotation(mr.value) }
                .toList()
        }

        private fun parseSingleNotation(notation: String): NvimKeyNotation? {
            val text = notation.trim()
            // <Xxx> may have modifiers like <C-A-x>
            if (text.startsWith("<") && text.endsWith(">")) {
                // Special handing for <Nop>
                if (text.equals("<Nop>", ignoreCase = true)) {
                    return NvimKeyNotation(emptyList(), "Nop")
                }

                val inner = text.substring(1, text.length - 1)
                val parts = inner.split("-")

                val rawKey = parts.last()
                val key =
                    supportedKeys.firstOrNull { it.nvimName.equals(rawKey, ignoreCase = true) }
                        ?.nvimName
                        ?: run {
                            logger.warn("Unknown key: $rawKey")
                            return null
                        }

                val mods =
                    parts.dropLast(1).map { token ->
                        NvimKeyModifier.fromString(token)
                            ?: run {
                                logger.warn("Unknown modifier: $token")
                                return null
                            }
                    }
                return NvimKeyNotation(mods, key)
            } else {
                // <Xxx>(Yyy) or any single character
                return NvimKeyNotation(emptyList(), text)
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
            modifiers.isNotEmpty() -> "<${modifiers.joinToString("-") { it.nvimPrefix }}-$key>"

            key.length == 1 -> key

            key.startsWith("<") -> key

            else -> "<$key>"
        }
    }
}
