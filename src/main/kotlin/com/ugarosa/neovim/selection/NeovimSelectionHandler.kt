package com.ugarosa.neovim.selection

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.ugarosa.neovim.rpc.event.VisualSelectionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NeovimSelectionHandler(
    private val editor: Editor,
) {
    private val logger = thisLogger()
    private val globalScheme = EditorColorsManager.getInstance().globalScheme
    private val selectionColor = globalScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
    private val attributes =
        TextAttributes().apply {
            backgroundColor = selectionColor
        }
    private val highlighters = mutableListOf<RangeHighlighter>()
    private val mutex = Mutex()

    suspend fun applyVisualSelectionEvent(event: VisualSelectionEvent) {
        logger.trace("applyVisualSelectionEvent: $event")

        resetSelection()

        val offsets =
            event.regions.map { region ->
                val startOffset = region.startOffset(editor.document)
                val endOffset = region.endOffset(editor.document)
                startOffset to endOffset
            }

        logger.trace("Offsets: $offsets")

        withContext(Dispatchers.EDT) {
            mutex.withLock {
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
    }

    suspend fun resetSelection() {
        withContext(Dispatchers.EDT) {
            mutex.withLock {
                highlighters.forEach {
                    editor.markupModel.removeHighlighter(it)
                }
                highlighters.clear()
            }
        }
    }
}
