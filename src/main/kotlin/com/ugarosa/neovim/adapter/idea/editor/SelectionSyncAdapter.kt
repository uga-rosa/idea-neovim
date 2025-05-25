package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.ugarosa.neovim.bus.VisualSelectionChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelectionSyncAdapter(
    private val editor: EditorEx,
) {
    private val attributes =
        TextAttributes().apply {
            val globalScheme = EditorColorsManager.getInstance().globalScheme
            val selectionColor = globalScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
            backgroundColor = selectionColor
        }
    private val highlighters = mutableListOf<RangeHighlighter>()

    suspend fun apply(event: VisualSelectionChanged) {
        val offsets =
            event.regions.map { region ->
                val startOffset = region.startOffset(editor.document)
                val endOffset = region.endOffset(editor.document)
                startOffset to endOffset
            }

        withContext(Dispatchers.EDT) {
            reset()

            offsets.forEach { (startOffset, endOffset) ->
                val highlighter =
                    editor.markupModel.addRangeHighlighter(
                        startOffset,
                        endOffset,
                        HighlighterLayer.SELECTION,
                        attributes,
                        HighlighterTargetArea.EXACT_RANGE,
                    )
                highlighters.add(highlighter)
            }
        }
    }

    fun reset() {
        highlighters.forEach { editor.markupModel.removeHighlighter(it) }
        highlighters.clear()
    }
}
