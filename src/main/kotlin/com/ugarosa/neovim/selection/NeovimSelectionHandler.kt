package com.ugarosa.neovim.selection

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.event.VisualSelectionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimSelectionHandler(
    private val editor: Editor,
) {
    private val logger = myLogger()
    private val attributes =
        TextAttributes().apply {
            val globalScheme = EditorColorsManager.getInstance().globalScheme
            val selectionColor = globalScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
            backgroundColor = selectionColor
        }
    private val highlighters = mutableListOf<RangeHighlighter>()

    suspend fun applyVisualSelectionEvent(event: VisualSelectionEvent) {
        logger.trace("applyVisualSelectionEvent: $event")

        val offsets =
            event.regions.map { region ->
                val startOffset = region.startOffset(editor.document)
                val endOffset = region.endOffset(editor.document)
                startOffset to endOffset
            }

        logger.trace("Offsets: $offsets")

        withContext(Dispatchers.EDT) {
            resetSelection()

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

    suspend fun resetSelection() {
        withContext(Dispatchers.EDT) {
            highlighters.forEach {
                editor.markupModel.removeHighlighter(it)
            }
            highlighters.clear()
        }
    }
}
