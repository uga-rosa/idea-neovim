package com.ugarosa.neovim.message

import com.intellij.openapi.components.Service
import com.ugarosa.neovim.common.BaseTextPane

@Service(Service.Level.PROJECT)
class MessageLivePane : BaseTextPane() {
    companion object {
        const val DISPLAY_NAME = "Live"
    }

    private var kind = MessageKind.Unknown
    private val chunks = mutableListOf<MsgChunk>()

    fun updateModel(
        show: MessageEvent.Show? = null,
        history: MessageEvent.ShowHistory? = null,
    ) {
        show?.let {
            this.kind = it.kind
            if (it.replaceLast) {
                this.chunks.clear()
            }
            this.chunks.addAll(it.content)
        }
        history?.let {
            this.kind = MessageKind.History
            this.chunks.clear()
            this.chunks.addAll(it.entries.flatMap { it.content })
        }
    }

    fun clear() {
        this.kind = MessageKind.Unknown
        this.chunks.clear()
    }

    fun isHide(): Boolean = chunks.isEmpty()

    fun flush() {
        styledDocument.remove(0, styledDocument.length)

        chunks.forEachIndexed { i, chunk ->
            val style = highlightManager.get(chunk.attrId).toAttributeSet()
            styledDocument.insertString(styledDocument.length, chunk.text, style)
            if (i < chunks.size - 1) {
                styledDocument.insertString(styledDocument.length, "\n", null)
            }
        }
    }
}
