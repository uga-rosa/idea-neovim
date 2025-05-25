package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.bus.NvimBufLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentSyncAdapter(
    private val editor: EditorEx,
    private val documentListener: IdeaDocumentListener,
) {
    private val doc = editor.document

    suspend fun apply(events: List<NvimBufLines>) {
        withContext(Dispatchers.EDT) {
            WriteCommandAction.writeCommandAction(editor.project)
                .run<Exception> {
                    documentListener.disable()
                    events.forEach { applyEvent(it) }
                    documentListener.enable()
                }
        }
    }

    private fun applyEvent(e: NvimBufLines) {
        val totalLines = doc.lineCount

        // Compute start/end offsets and flag for trailing newline
        val (startOffset, endOffset, addTrailingNewline) =
            when {
                // 1) Append at end of document
                e.firstLine >= totalLines ->
                    Triple(doc.textLength, doc.textLength, false)

                // 2) Replacement range includes end of document
                e.lastLine == -1 || e.lastLine >= totalLines -> {
                    val start = (doc.getLineStartOffset(e.firstLine) - 1).coerceAtLeast(0)
                    Triple(start, doc.textLength, false)
                }

                // 3) Range within document
                else -> {
                    val start = doc.getLineStartOffset(e.firstLine)
                    val end = doc.getLineStartOffset(e.lastLine)
                    Triple(start, end, true)
                }
            }

        val replacementText =
            if (e.replacementLines.isEmpty()) {
                ""
            } else {
                buildString {
                    if (!addTrailingNewline) append("\n")
                    append(e.replacementLines.joinToString("\n"))
                    if (addTrailingNewline) append("\n")
                }
            }

        doc.replaceString(startOffset, endOffset, replacementText)
    }
}
