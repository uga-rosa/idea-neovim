package com.ugarosa.neovim.adapter.idea.ui.message

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.project.Project
import com.ugarosa.neovim.adapter.idea.ui.BaseEditorViewer

private const val MAX_CHARS = 50_000
private const val TRIM_CHARS = 5_000

@Service(Service.Level.PROJECT)
class MessageHistoryView(
    project: Project,
) : BaseEditorViewer(project) {
    companion object {
        const val TAB_TITLE = "History"
    }

    fun updateHistory(show: MessageEvent.Show) {
        if (!show.history) return

        runUndoTransparentWriteAction {
            val markup = editor.markupModel

            if (document.textLength > 0) {
                document.insertString(document.textLength, "\n")
            }

            show.content.forEach { chunk ->
                val start = document.textLength
                val text = chunk.text
                document.insertString(start, text)
                val end = start + text.length

                val attrs = highlightManager.get(chunk.attrId).toTextAttributes()
                markup.addRangeHighlighter(
                    start,
                    end,
                    HighlighterLayer.SYNTAX,
                    attrs,
                    HighlighterTargetArea.EXACT_RANGE,
                )
            }

            if (document.textLength > MAX_CHARS) {
                document.deleteString(0, TRIM_CHARS)
            }
        }
    }
}
