package com.ugarosa.neovim.keymap.notation

import com.ugarosa.neovim.logger.MyLogger
import java.awt.event.KeyEvent

private val logger = MyLogger.getInstance("com.ugarosa.neovim.keymap.notation.SupportedKey")

/**
 * Supported key mapping and printability in a single source-of-truth list.
 */
data class SupportedKey(
    val awtName: String,
    val neovimName: String,
    // Whether this key is considered a printable character (i.e. should be passed through to KEY_TYPED)
    val printable: Boolean,
) {
    val awtKeyCode: Int =
        try {
            KeyEvent::class.java.getField("VK_$awtName").getInt(null)
        } catch (e: Exception) {
            logger.warn("Failed to get AWT key code for $awtName", e)
            -1
        }
}

// Single source of truth for all supported keys:
val supportedKeys: List<SupportedKey> =
    buildList {
        // Letters A–Z
        addAll(('A'..'Z').map { SupportedKey(it.toString(), it.toString(), printable = true) })
        // Digits 0–9
        addAll(('0'..'9').map { SupportedKey(it.toString(), it.toString(), printable = true) })
        // Function keys F1–F12 (non-printable)
        addAll((1..12).map { SupportedKey("F$it", "F$it", printable = false) })
        // Numpad keys (digits & operators)
        addAll((0..9).map { SupportedKey("NUMPAD$it", "k$it", printable = true) })
        addAll(
            listOf(
                SupportedKey("DECIMAL", "kPoint", printable = true),
                SupportedKey("ADD", "kPlus", printable = true),
                SupportedKey("SUBTRACT", "kMinus", printable = true),
                SupportedKey("MULTIPLY", "kMultiply", printable = true),
                SupportedKey("DIVIDE", "kDivide", printable = true),
                SupportedKey("SEPARATOR", "kComma", printable = true),
            ),
        )
        // Common punctuation and symbols
        addAll(
            listOf(
                SupportedKey("MINUS", "-", printable = true),
                SupportedKey("EQUALS", "=", printable = true),
                SupportedKey("BACK_SLASH", "\\", printable = true),
                SupportedKey("BACK_QUOTE", "`", printable = true),
                SupportedKey("OPEN_BRACKET", "[", printable = true),
                SupportedKey("CLOSE_BRACKET", "]", printable = true),
                SupportedKey("SEMICOLON", ";", printable = true),
                SupportedKey("QUOTE", "'", printable = true),
                SupportedKey("COMMA", ",", printable = true),
                SupportedKey("PERIOD", ".", printable = true),
                SupportedKey("SLASH", "/", printable = true),
                SupportedKey("SPACE", "Space", printable = true),
            ),
        )
        // Shift‐modified symbols
        addAll(
            listOf(
                SupportedKey("EXCLAMATION_MARK", "!", printable = true),
                SupportedKey("AT", "@", printable = true),
                SupportedKey("NUMBER_SIGN", "#", printable = true),
                SupportedKey("DOLLAR", "$", printable = true),
                SupportedKey("CIRCUMFLEX", "^", printable = true),
                SupportedKey("AMPERSAND", "&", printable = true),
                SupportedKey("ASTERISK", "*", printable = true),
                SupportedKey("LEFT_PARENTHESIS", "(", printable = true),
                SupportedKey("RIGHT_PARENTHESIS", ")", printable = true),
                SupportedKey("UNDERSCORE", "_", printable = true),
                SupportedKey("PLUS", "+", printable = true),
                SupportedKey("BRACELEFT", "{", printable = true),
                SupportedKey("BRACERIGHT", "}", printable = true),
                SupportedKey("COLON", ":", printable = true),
                SupportedKey("QUOTEDBL", "\"", printable = true),
                SupportedKey("LESS", "<lt>", printable = true),
                SupportedKey("GREATER", ">", printable = true),
            ),
        )
        // Other common keys (non-printable)
        addAll(
            listOf(
                SupportedKey("ENTER", "CR", printable = false),
                SupportedKey("BACK_SPACE", "BS", printable = false),
                SupportedKey("TAB", "Tab", printable = false),
                SupportedKey("ESCAPE", "Esc", printable = false),
                SupportedKey("DELETE", "Del", printable = false),
                SupportedKey("INSERT", "Ins", printable = false),
                SupportedKey("HOME", "Home", printable = false),
                SupportedKey("END", "End", printable = false),
                SupportedKey("PAGE_UP", "PageUp", printable = false),
                SupportedKey("PAGE_DOWN", "PageDown", printable = false),
                SupportedKey("UP", "Up", printable = false),
                SupportedKey("DOWN", "Down", printable = false),
                SupportedKey("LEFT", "Left", printable = false),
                SupportedKey("RIGHT", "Right", printable = false),
            ),
        )
    }

// Derive the set of printable key codes when needed:
val printableVKs: Set<Int> = supportedKeys.filter { it.printable }.map { it.awtKeyCode }.toSet()
