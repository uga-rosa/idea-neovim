package com.ugarosa.neovim.session.document

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class NeovimDocumentListener(
    private val handler: NeovimDocumentHandler,
) : DocumentListener {
    override fun beforeDocumentChange(event: DocumentEvent) {
        handler.syncDocumentChange(event)
    }
}
