package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.bus.IdeaDocumentChange
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.domain.id.BufferId
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
        val startOffset = event.offset
        val endOffset = startOffset + event.oldLength
        val replacement = event.newFragment.toString().replace("\r\n", "\n").split("\n")
        val change =
            IdeaDocumentChange(
                bufferId = bufferId,
                start = NvimPosition.fromOffset(startOffset, editor.document),
                end = NvimPosition.fromOffset(endOffset, editor.document),
                replacement = replacement,
            )
        bus.tryEmit(change)
    }

    override fun dispose() {
        disable()
    }
}
