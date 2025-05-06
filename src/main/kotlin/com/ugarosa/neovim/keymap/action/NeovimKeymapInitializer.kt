package com.ugarosa.neovim.keymap.action

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.ugarosa.neovim.keymap.notation.supportedKeys
import javax.swing.KeyStroke

fun initializeKeymap() {
    val keymap = KeymapManager.getInstance().activeKeymap
    // Build modifier combos
    val modifierPrefixes =
        listOf("", "ctrl", "alt", "shift", "meta").flatMap { a ->
            listOf("", "ctrl", "alt", "shift", "meta").mapNotNull { b ->
                listOf(a, b).filter { it.isNotEmpty() }.distinct().takeIf { it.isNotEmpty() }?.joinToString(" ")
            } + listOf("")
        }.distinct()

    // Generate shortcuts from supportedKeys
    supportedKeys.forEach { sk ->
        modifierPrefixes.forEach { prefix ->
            // Ignore empty prefixes for single keystrokes (e.g. "A")
            if (sk.awtName.length == 1 && prefix.isEmpty()) return@forEach

            val combo = if (prefix.isEmpty()) sk.awtName else "$prefix ${sk.awtName}"
            KeyStroke.getKeyStroke(combo)?.let { ks ->
                keymap.addShortcut(NEOVIM_KEY_ACTION_ID, KeyboardShortcut(ks, null))
            }
        }
    }
}
