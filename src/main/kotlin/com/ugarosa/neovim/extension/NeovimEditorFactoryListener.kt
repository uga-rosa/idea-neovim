package com.ugarosa.neovim.extension

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

@Deprecated("User NeovimKeyAction and NeovimKeymapInitializer instead")
class NeovimEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        editor.contentComponent.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val session =
                        editor.getUserData(NEOVIM_SESSION_KEY)
                            ?: error("NeovimEditorSession does not exist")

                    val key = neovimNotation(e)
                    if (key.length <= 1) {
                        super.keyPressed(e)
                    } else {
                        session.sendKeyAndSyncStatus(key)
                        e.consume()
                    }
                }
            },
        )
    }

    private fun neovimNotation(e: KeyEvent): String {
        when (e.keyCode) {
            KeyEvent.VK_CONTROL,
            KeyEvent.VK_SHIFT,
            KeyEvent.VK_ALT,
            KeyEvent.VK_META,
            -> return ""
        }

        val modifiers =
            buildList {
                if (e.isControlDown) add("C")
                if (e.isShiftDown) add("S")
                if (e.isAltDown) add("A")
            }
        val key = KeyEvent.getKeyText(e.keyCode).lowercase()

        return if (modifiers.isNotEmpty()) {
            "<${modifiers.joinToString("-")}-$key>"
        } else if (key.length == 1) {
            key
        } else {
            "<$key>"
        }
    }
}
