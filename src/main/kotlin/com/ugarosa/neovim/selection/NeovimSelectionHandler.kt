package com.ugarosa.neovim.selection

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.ugarosa.neovim.common.NeovimPosition
import com.ugarosa.neovim.common.toLogicalPosition
import com.ugarosa.neovim.rpc.event.VisualMode
import com.ugarosa.neovim.rpc.event.VisualSelectionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimSelectionHandler(
    private val editor: Editor,
) {
    private val logger = thisLogger()

    suspend fun applyVisualSelectionEvent(event: VisualSelectionEvent) {
        logger.trace("applyVisualSelectionEvent: $event")
        when (event.mode) {
            VisualMode.VISUAL -> selectWord(event)
            VisualMode.VISUAL_LINE -> selectLine(event)
            VisualMode.VISUAL_BLOCK -> selectBlock(event)
        }
    }

    private suspend fun selectWord(event: VisualSelectionEvent) {
        val start = event.startPosition.toLogicalPosition(editor.document)
        val startOffset = editor.logicalPositionToOffset(start)
        val end = event.endPosition.toLogicalPosition(editor.document)
        val endOffset = editor.logicalPositionToOffset(end)

        withContext(Dispatchers.EDT) {
            editor.selectionModel.setSelection(startOffset, endOffset)
        }
    }

    private suspend fun selectLine(event: VisualSelectionEvent) {
        val startOffset = editor.document.getLineStartOffset(event.startPosition.line)
        val endLine = event.endPosition.line.coerceAtMost(editor.document.lineCount - 1)
        val endOffset = editor.document.getLineEndOffset(endLine)

        withContext(Dispatchers.EDT) {
            editor.selectionModel.setSelection(startOffset, endOffset)
        }
    }

    private suspend fun selectBlock(event: VisualSelectionEvent) {
        val startRow = minOf(event.startPosition.row, event.endPosition.row)
        val endRow = maxOf(event.startPosition.row, event.endPosition.row)
        val colStartByte = minOf(event.startPosition.col, event.endPosition.col)
        val colEndByte = maxOf(event.startPosition.col, event.endPosition.col)

        val offsets =
            (startRow..endRow).map { row ->
                val start = NeovimPosition(row, colStartByte).toLogicalPosition(editor.document)
                val end = NeovimPosition(row, colEndByte).toLogicalPosition(editor.document)
                Triple(row - 1, start, end)
            }

        withContext(Dispatchers.EDT) {
            val caretModel = editor.caretModel
            val mainCaret = caretModel.primaryCaret
            resetSelection()

            for ((line, start, end) in offsets) {
                val startOffset = editor.logicalPositionToOffset(start)
                val endOffset = editor.logicalPositionToOffset(end)
                val caret =
                    if (line == mainCaret.logicalPosition.line) {
                        mainCaret
                    } else {
                        caretModel.addCaret(VisualPosition(line, start.column))
                    }
                caret?.setSelection(startOffset, endOffset)
            }
        }
    }

    suspend fun resetSelection() {
        withContext(Dispatchers.EDT) {
            editor.selectionModel.removeSelection()
            editor.caretModel.removeSecondaryCarets()
        }
    }
}
