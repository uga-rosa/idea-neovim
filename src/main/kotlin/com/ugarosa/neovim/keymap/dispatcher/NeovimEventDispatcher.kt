package com.ugarosa.neovim.keymap.dispatcher

import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
import com.ugarosa.neovim.keymap.notation.printableVKs
import com.ugarosa.neovim.keymap.router.NeovimKeyRouter
import java.awt.AWTEvent
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

class NeovimEventDispatcher(
    private val keyRouter: NeovimKeyRouter,
) : IdeEventQueue.EventDispatcher {
    private val logger = thisLogger()

    override fun dispatch(e: AWTEvent): Boolean {
        if (e is KeyEvent) {
            return hijackKeyEvent(e)
        }

        return false
    }

    // Store the last modifiers for KEY_TYPED events
    private var lastMods = 0

    // Hijack all key events
    private fun hijackKeyEvent(e: KeyEvent): Boolean {
        logger.trace("Received key event: $e")

        val editor = searchEditor()

        when (e.id) {
            KeyEvent.KEY_PRESSED -> {
                val mods = e.modifiersEx
                lastMods = mods

                // Pure characters or SHIFT + character go to KEY_TYPED
                val onlyShift = mods == KeyEvent.SHIFT_DOWN_MASK
                if (e.keyCode in printableVKs && (mods == 0 || onlyShift)) {
                    return false
                }

                NeovimKeyNotation.fromKeyPressedEvent(e)?.also {
                    logger.debug("Pressed key: $it")
                    return keyRouter.enqueueKey(it, editor)
                }
                // Fallback to default behavior if not supported key
                return false
            }

            KeyEvent.KEY_TYPED -> {
                val c = e.keyChar
                val mods = lastMods

                // CTRL/ALT/META + character are not handled by KEY_TYPED
                if (mods and (KeyEvent.CTRL_DOWN_MASK or KeyEvent.ALT_DOWN_MASK or KeyEvent.META_DOWN_MASK) != 0) {
                    logger.trace("Ignore KEY_TYPED event with modifiers: $e")
                    return true
                }

                // Special case for space
                if (c == ' ') {
                    val notation = NeovimKeyNotation.fromModsAndKey(mods, "Space")
                    logger.debug("Pressed key: $notation")
                    return keyRouter.enqueueKey(notation, editor)
                }

                NeovimKeyNotation.fromKeyTypedEvent(e)?.also {
                    logger.debug("Typed key: $it")
                    return keyRouter.enqueueKey(it, editor)
                }
                return true
            }

            else -> {
                return false
            }
        }
    }

    private fun searchEditor(): Editor? {
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        return focusOwner?.let { DataManager.getInstance().getDataContext(it) }
            ?.let { CommonDataKeys.EDITOR.getData(it) }
    }
}
