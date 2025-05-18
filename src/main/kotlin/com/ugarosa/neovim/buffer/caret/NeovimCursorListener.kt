package com.ugarosa.neovim.buffer.caret

import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener

class NeovimCursorListener(
    private val handler: NeovimCursorHandler,
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        handler.syncIdeaToNeovim()
    }
}
