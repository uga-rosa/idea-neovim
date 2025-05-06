package com.ugarosa.neovim.document

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NeovimDocumentListener(
    private val scope: CoroutineScope,
    private val handler: NeovimDocumentHandler,
) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        scope.launch {
            handler.syncDocumentChange(event)
        }
    }
}
