package com.ugarosa.neovim.adapter.idea.input.dispatcher

import com.intellij.ide.IdeEventQueue
import com.ugarosa.neovim.adapter.idea.input.notation.NeovimKeyNotation
import com.ugarosa.neovim.adapter.idea.input.notation.printableVKs
import com.ugarosa.neovim.adapter.idea.input.router.NvimKeyRouter
import com.ugarosa.neovim.common.unsafeFocusEditor
import com.ugarosa.neovim.logger.myLogger
import java.awt.AWTEvent
import java.awt.event.KeyEvent

class NvimKeyEventDispatcher(
    private val keyRouter: NvimKeyRouter,
) : IdeEventQueue.EventDispatcher {
    private val logger = myLogger()

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

        // Ignore events not from the editor
        val editor = unsafeFocusEditor() ?: return false

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
                    logger.debug("Typed Space key: $notation")
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
}
