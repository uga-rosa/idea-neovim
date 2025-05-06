package com.ugarosa.neovim.cursor

import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimCaretListener(
    private val scope: CoroutineScope,
    private val handler: NeovimCursorHandler,
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        scope.launch {
            handler.syncIdeaToNeovim()
        }
    }
}
