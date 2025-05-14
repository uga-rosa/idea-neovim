package com.ugarosa.neovim.message

import com.intellij.openapi.components.Service
import com.ugarosa.neovim.common.BaseTextPane

private const val MAX_CHARS = 50_000
private const val TRIM_CHARS = 5_000

@Service(Service.Level.PROJECT)
class MessageHistoryPane : BaseTextPane() {
    companion object {
        const val DISPLAY_NAME = "History"
    }

    fun updateHistory(show: MessageEvent.Show) {
        if (!show.history) return

        var offset = styledDocument.length
        if (offset > 0) {
            styledDocument.insertString(offset, "\n", null)
            offset++
        }

        show.content.forEach { chunk ->
            val style = highlightManager.get(chunk.attrId).toAttributeSet()
            styledDocument.insertString(offset, chunk.text, style)
            offset += chunk.text.length
        }

        if (styledDocument.length > MAX_CHARS) {
            styledDocument.remove(0, TRIM_CHARS)
        }
    }
}
