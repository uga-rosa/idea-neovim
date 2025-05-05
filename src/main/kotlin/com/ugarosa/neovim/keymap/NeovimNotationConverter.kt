package com.ugarosa.neovim.keymap

import java.awt.event.KeyEvent

fun KeyEvent.toNeovimNotation(): String {
    when (keyCode) {
        KeyEvent.VK_CONTROL,
        KeyEvent.VK_SHIFT,
        KeyEvent.VK_ALT,
        KeyEvent.VK_META,
        -> return ""
    }

    val modifiers =
        buildList {
            if (isControlDown) add("C")
            if (isShiftDown) add("S")
            if (isAltDown) add("A")
            if (isMetaDown) add("M")
        }
    val key = KeyEvent.getKeyText(keyCode)

    return if (modifiers.isNotEmpty()) {
        "<${modifiers.joinToString("-")}-$key>"
    } else if (key.length == 1) {
        key.lowercase()
    } else {
        "<$key>"
    }
}
