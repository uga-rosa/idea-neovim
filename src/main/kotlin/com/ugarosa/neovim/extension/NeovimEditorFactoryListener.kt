package com.ugarosa.neovim.extension

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.ugarosa.neovim.common.neovimNotation
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
}
