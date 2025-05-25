package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.bus.IdeaCaretMoved
import com.ugarosa.neovim.bus.IdeaToNvimBus
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition

class IdeaCaretListener(
    private val bufferId: BufferId,
    private val editor: EditorEx,
) : CaretListener, Disposable {
    private val bus = service<IdeaToNvimBus>()
    private var isEnabled = false

    fun enable() {
        if (isEnabled) return
        isEnabled = true
        editor.caretModel.addCaretListener(this, this)
    }

    fun disable() {
        if (!isEnabled) return
        isEnabled = false
        editor.caretModel.removeCaretListener(this)
    }

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val pos = NvimPosition.fromOffset(editor.caretModel.offset, editor.document)
        bus.tryEmit(IdeaCaretMoved(bufferId, pos))
    }

    override fun dispose() {
        disable()
    }
}
