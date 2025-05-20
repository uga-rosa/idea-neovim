package com.ugarosa.neovim.buffer.document

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.bufferAttach
import com.ugarosa.neovim.rpc.client.api.bufferSetLines
import com.ugarosa.neovim.rpc.client.api.bufferSetText
import com.ugarosa.neovim.rpc.client.api.changedTick
import com.ugarosa.neovim.rpc.client.api.input
import com.ugarosa.neovim.rpc.client.api.modifiable
import com.ugarosa.neovim.rpc.client.api.noModifiable
import com.ugarosa.neovim.rpc.client.api.setFiletype
import com.ugarosa.neovim.rpc.event.handler.BufLinesEvent
import com.ugarosa.neovim.rpc.type.NeovimPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class NeovimDocumentHandler private constructor(
    scope: CoroutineScope,
    private val bufferId: BufferId,
    private val editor: Editor,
) : Disposable {
    private val logger = myLogger()
    private val client = service<NeovimClient>()
    private val documentListenerGuard =
        ListenerGuard(
            NeovimDocumentListener(this),
            { editor.document.addDocumentListener(it) },
            { editor.document.removeDocumentListener(it) },
        )
    private val ignoreChangedTicks = ConcurrentHashMap.newKeySet<Long>()

    private val docChangeChannel = Channel<DocChange>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (docChange in docChangeChannel) {
                client.changedTick(bufferId)
                    .also { ignoreChangedTicks.add(it + 1) }
                when (docChange) {
                    is DocChange.Input -> client.input(docChange.text)

                    is DocChange.SetText ->
                        client.bufferSetText(
                            bufferId,
                            docChange.start,
                            docChange.end,
                            docChange.replacement,
                        )
                }
            }
        }
    }

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            bufferId: BufferId,
            editor: Editor,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(scope, bufferId, editor)
            handler.initializeBuffer()
            handler.enableListener()
            return handler
        }
    }

    private suspend fun initializeBuffer() {
        val lines = editor.document.text.split("\n")
        client.bufferSetLines(bufferId, 0, -1, lines)

        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        if (virtualFile != null && virtualFile.isInLocalFileSystem) {
            client.setFiletype(bufferId, virtualFile.path)
        }

        changeModifiable()

        client.bufferAttach(bufferId)
    }

    suspend fun changeModifiable() {
        if (isWritable()) {
            client.modifiable(bufferId)
        } else {
            client.noModifiable(bufferId)
        }
    }

    private fun isWritable() = !editor.isViewer && editor.document.isWritable

    private fun enableListener() {
        logger.trace("Enabling document listener for buffer: $bufferId")
        documentListenerGuard.register()
    }

    suspend fun applyBufferLinesEvent(e: BufLinesEvent) {
        if (ignoreChangedTicks.remove(e.changedTick)) {
            logger.trace("Ignore event by changed tick: $e")
            return
        }
        logger.trace("Apply buffer lines event: $e")

        withContext(Dispatchers.EDT) {
            WriteCommandAction.writeCommandAction(editor.project)
                .run<Exception> {
                    applyLinesToDocument(editor.document, e)
                }
        }
    }

    private fun applyLinesToDocument(
        doc: Document,
        e: BufLinesEvent,
    ) {
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

        documentListenerGuard.runWithoutListener {
            doc.replaceString(startOffset, endOffset, replacementText)
        }
    }

    // Must be called before the document is changed
    fun syncDocumentChange(event: DocumentEvent) {
        if (getMode().isInsert() && isSingleLineChange(event)) {
            sendInput(event)
        } else {
            sendBufferSetText(event)
        }
    }

    private fun isSingleLineChange(event: DocumentEvent): Boolean {
        // Compute the range of the change and the cursor position.
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength
        // Ensure the caret lies within the deleted range.
        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset
        if (beforeDelete < 0 || afterDelete < 0) {
            return false
        }
        // Change must stay on a single line.
        val startRow = event.document.getLineNumber(startOffset)
        val endRow = event.document.getLineNumber(endOffset)
        if (startRow != endRow) {
            return false
        }
        // Inserted text must not contain newlines.
        val insertText = event.newFragment.toString()
        if (insertText.contains("\n")) {
            return false
        }
        // Deletion range must be fully inside the same line boundaries.
        val lineStartOffset = event.document.getLineStartOffset(startRow)
        val lineEndOffset = event.document.getLineEndOffset(startRow)
        if (caretOffset - lineStartOffset < beforeDelete) {
            return false
        }
        if (lineEndOffset - caretOffset < afterDelete) {
            return false
        }
        // Verify that the number of Unicode code points matches the expected length.
        val deleteText = event.document.getText(TextRange(startOffset, endOffset))
        if (deleteText.codePointCount(0, deleteText.length) != event.oldLength) {
            return false
        }
        if (insertText.codePointCount(0, insertText.length) != event.newLength) {
            return false
        }

        return true
    }

    private val beforeDeleteStr = "<C-g>U<Left><Del>"
    private val afterDeleteStr = "<Del>"

    // Apply oneline text change with dot-repeat
    private fun sendInput(event: DocumentEvent) {
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength

        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset

        val deleteStr = beforeDeleteStr.repeat(beforeDelete) + afterDeleteStr.repeat(afterDelete)
        val insertStr = event.newFragment.toString()

        val change = DocChange.Input(deleteStr + insertStr)
        docChangeChannel.trySend(change)
    }

    // Apply multiline text change
    private fun sendBufferSetText(event: DocumentEvent) {
        val start = NeovimPosition.fromOffset(event.offset, editor.document)
        val endOffset = event.offset + event.oldLength
        val end = NeovimPosition.fromOffset(endOffset, editor.document)
        val replacement =
            event.newFragment.toString()
                .replace("\r\n", "\n")
                .split("\n")

        val change = DocChange.SetText(start, end, replacement)
        docChangeChannel.trySend(change)
    }

    override fun dispose() {
        documentListenerGuard.unregister()
    }
}
