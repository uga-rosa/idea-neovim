package com.ugarosa.neovim.buffer.caret

import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener

class NeovimCaretListener(
    private val handler: NeovimCaretHandler,
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        handler.syncIdeaToNeovim()
    }
}
