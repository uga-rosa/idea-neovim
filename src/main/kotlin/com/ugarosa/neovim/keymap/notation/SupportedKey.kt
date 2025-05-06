package com.ugarosa.neovim.keymap.notation

import com.intellij.openapi.diagnostic.Logger
import java.awt.event.KeyEvent

private val logger = Logger.getInstance("com.ugarosa.neovim.keymap.notation.SupportedKey")

data class SupportedKey(
    val awtName: String,
    val neovimName: String,
) {
    val awtKeyCode: Int =
        try {
            KeyEvent::class.java.getField("VK_$awtName").getInt(null)
        } catch (e: Exception) {
            logger.warn("Failed to get AWT key code for $awtName", e)
            -1
        }
}

/**
 * Single source of truth for all supported keys and their mappings.
 */
val supportedKeys: List<SupportedKey> =
    buildList {
        // Letters A–Z
        addAll(('A'..'Z').map { SupportedKey(it.toString(), it.toString()) })
        // Digits 0–9
        addAll(('0'..'9').map { SupportedKey(it.toString(), it.toString()) })
        // Function keys F1–F12
        addAll((1..12).map { SupportedKey("F$it", "F$it") })
        // Numpad keys k0–k9
        addAll((0..9).map { SupportedKey("NUMPAD$it", "k$it") })
        // Numpad operators and point
        addAll(
            listOf(
                SupportedKey("DECIMAL", "kPoint"),
                SupportedKey("ADD", "kPlus"),
                SupportedKey("SUBTRACT", "kMinus"),
                SupportedKey("MULTIPLY", "kMultiply"),
                SupportedKey("DIVIDE", "kDivide"),
                SupportedKey("SEPARATOR", "kComma"),
            ),
        )
        // Other common keys
        addAll(
            listOf(
                SupportedKey("SPACE", "Space"),
                SupportedKey("ENTER", "CR"),
                SupportedKey("BACK_SPACE", "BS"),
                SupportedKey("TAB", "Tab"),
                SupportedKey("ESCAPE", "Esc"),
                SupportedKey("DELETE", "Del"),
                SupportedKey("INSERT", "Ins"),
                SupportedKey("HOME", "Home"),
                SupportedKey("END", "End"),
                SupportedKey("PAGE_UP", "PageUp"),
                SupportedKey("PAGE_DOWN", "PageDown"),
                SupportedKey("UP", "Up"),
                SupportedKey("DOWN", "Down"),
                SupportedKey("LEFT", "Left"),
                SupportedKey("RIGHT", "Right"),
            ),
        )
    }
