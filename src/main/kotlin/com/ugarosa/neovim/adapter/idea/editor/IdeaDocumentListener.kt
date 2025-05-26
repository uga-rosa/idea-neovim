package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.bus.IdeaDocumentChanged
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.mode.getMode
import com.ugarosa.neovim.domain.position.NvimPosition

class IdeaDocumentListener(
    private val bufferId: BufferId,
    private val editor: EditorEx,
) : DocumentListener, Disposable {
    private val bus = service<IdeaToNvimBus>()
    private var isEnabled = false

    fun enable() {
        if (isEnabled) return
        isEnabled = true
        editor.document.addDocumentListener(this, this)
    }

    fun disable() {
        if (!isEnabled) return
        isEnabled = false
        editor.document.removeDocumentListener(this)
    }

    override fun beforeDocumentChange(event: DocumentEvent) {
        val documentChanged = translateEvent(event)
        bus.tryEmit(documentChanged)
    }

    private fun translateEvent(event: DocumentEvent): IdeaDocumentChanged {
        return if (getMode().isInsert() && isNearCursor(event)) {
            nearCursorChange(event)
        } else {
            farCursorChange(event)
        }
    }

    private fun isNearCursor(event: DocumentEvent): Boolean {
        // Compute the range of the change and the cursor position.
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength
        // Ensure the caret lies within the deleted range.
        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset
        return !(beforeDelete < 0 || afterDelete < 0)
    }

    private fun nearCursorChange(event: DocumentEvent): IdeaDocumentChanged.NearCursor {
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength

        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset

        val text =
            event.newFragment.toString()
                .replace("\r\n", "\n")

        return IdeaDocumentChanged.NearCursor(
            bufferId = bufferId,
            caretOffset = caretOffset,
            beforeDelete = beforeDelete,
            afterDelete = afterDelete,
            text = text,
        )
    }

    private fun farCursorChange(event: DocumentEvent): IdeaDocumentChanged.FarCursor {
        val start = NvimPosition.fromOffset(event.offset, editor.document)
        val endOffset = event.offset + event.oldLength
        val end = NvimPosition.fromOffset(endOffset, editor.document)
        val replacement =
            event.newFragment.toString()
                .replace("\r\n", "\n")
                .split("\n")

        return IdeaDocumentChanged.FarCursor(
            bufferId = bufferId,
            start = start,
            end = end,
            replacement = replacement,
        )
    }

    override fun dispose() {
        disable()
    }
}
