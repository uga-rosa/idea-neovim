package com.ugarosa.neovim.keymap.dispatcher

import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.keymap.notation.NeovimKeyNotation
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

        val editor =
            searchEditor() ?: run {
                logger.warn("No editor found for key event")
                return false
            }

        when (e.id) {
            KeyEvent.KEY_PRESSED -> {
                val c = e.keyChar
                val mods = e.modifiersEx

                lastMods = mods

                // Pure characters or SHIFT + character go to KEY_TYPED
                val onlyShift = mods == KeyEvent.SHIFT_DOWN_MASK
                if (c != KeyEvent.CHAR_UNDEFINED && !c.isISOControl() && (mods == 0 || onlyShift)) {
                    return false
                }

                NeovimKeyNotation.fromKeyPressedEvent(e)?.also {
                    logger.debug("Pressed key: $it")
                    keyRouter.enqueueKey(it, editor)
                    return true
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
                    NeovimKeyNotation.fromModsAndKey(mods, "Space").also {
                        logger.debug("Pressed key: $it")
                        keyRouter.enqueueKey(it, editor)
                    }
                    return true
                }

                NeovimKeyNotation.fromKeyTypedEvent(e)?.also {
                    logger.debug("Typed key: $it")
                    keyRouter.enqueueKey(it, editor)
                }
                return true
            }

            else -> {
                return true
            }
        }
    }

    private fun searchEditor(): Editor? {
        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        return focusOwner?.let { DataManager.getInstance().getDataContext(it) }
            ?.let { CommonDataKeys.EDITOR.getData(it) }
    }
}
