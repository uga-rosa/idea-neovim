package com.ugarosa.neovim.cursor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.ugarosa.neovim.session.NEOVIM_SESSION_KEY

class NeovimCaretListener(
    private val editor: Editor,
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        editor.getUserData(NEOVIM_SESSION_KEY)
            ?.syncCursorFromIdeaToNeovim()
    }
}
