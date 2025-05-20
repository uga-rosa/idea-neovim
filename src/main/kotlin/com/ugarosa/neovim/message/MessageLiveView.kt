package com.ugarosa.neovim.message

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.common.BaseEditorViewer

@Service(Service.Level.PROJECT)
class MessageLiveView(
    project: Project,
) : BaseEditorViewer(project) {
    companion object {
        const val TAB_TITLE = "Live"
    }

    private var kind = MessageKind.Unknown
    private val chunks = mutableListOf<MsgChunk>()
    private var isDirty = false

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
        history?.let { it ->
            this.kind = MessageKind.History
            this.chunks.clear()
            this.chunks.addAll(it.entries.flatMap { it.content })
        }
        isDirty = true
    }

    fun clear() {
        this.kind = MessageKind.Unknown
        this.chunks.clear()
        isDirty = false
    }

    /**
     * Apply pending updates to the editor component.
     * @return true if something was rendered.
     */
    fun flush(): Boolean {
        if (!isDirty) return false
        isDirty = false

        runUndoTransparentWriteAction {
            // Set the text. It replaces the whole document.
            val text = chunks.joinToString("\n") { it.text }
            document.setText(text)

            // Remove all highlighters
            val markup = editor.markupModel
            markup.removeAllHighlighters()

            // Add new highlighters
            var offset = 0
            chunks.forEach { chunk ->
                val length = chunk.text.length
                val attrs = highlightManager.get(chunk.attrId).toTextAttributes()
                markup.addRangeHighlighter(
                    offset,
                    offset + length,
                    HighlighterLayer.SYNTAX,
                    attrs,
                    HighlighterTargetArea.EXACT_RANGE,
                )
                offset += length + 1 // +1 for the newline
            }
        }

        return true
    }
}
