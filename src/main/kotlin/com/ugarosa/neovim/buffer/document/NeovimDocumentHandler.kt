package com.ugarosa.neovim.buffer.document

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.bufferAttach
import com.ugarosa.neovim.rpc.client.api.bufferSetLines
import com.ugarosa.neovim.rpc.client.api.bufferSetText
import com.ugarosa.neovim.rpc.client.api.changedTick
import com.ugarosa.neovim.rpc.client.api.modifiable
import com.ugarosa.neovim.rpc.client.api.noModifiable
import com.ugarosa.neovim.rpc.client.api.paste
import com.ugarosa.neovim.rpc.client.api.sendDeletion
import com.ugarosa.neovim.rpc.client.api.setFiletype
import com.ugarosa.neovim.rpc.event.handler.BufLinesEvent
import com.ugarosa.neovim.rpc.type.NeovimPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class NeovimDocumentHandler private constructor(
    private val bufferId: BufferId,
    private val editor: Editor,
    private val sendChan: SendChannel<suspend () -> Unit>,
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

    companion object {
        suspend fun create(
            bufferId: BufferId,
            editor: Editor,
            sendChan: SendChannel<suspend () -> Unit>,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(bufferId, editor, sendChan)
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

    suspend fun handleBufferLinesEvents(events: List<BufLinesEvent>) =
        withContext(Dispatchers.EDT) {
            WriteCommandAction.writeCommandAction(editor.project)
                .run<Exception> {
                    events
                        .filter { !ignoreChangedTicks.remove(it.changedTick) }
                        .forEach { applyLinesToDocument(editor.document, it) }
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
        logger.trace("Sync document change: $event")
        if (getMode().isInsert() && isCursorPositionChange(event)) {
            sendInput(event)
        } else {
            sendBufferSetText(event)
        }
    }

    private fun isCursorPositionChange(event: DocumentEvent): Boolean {
        // Compute the range of the change and the cursor position.
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength
        // Ensure the caret lies within the deleted range.
        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset
        return !(beforeDelete < 0 || afterDelete < 0)
    }

    // Apply oneline text change with dot-repeat
    private fun sendInput(event: DocumentEvent) {
        val caretOffset = editor.caretModel.offset
        val startOffset = event.offset
        val endOffset = event.offset + event.oldLength

        val beforeDelete = caretOffset - startOffset
        val afterDelete = endOffset - caretOffset

        val text =
            event.newFragment.toString()
                .replace("\r\n", "\n")

        sendChan.trySend {
            client.changedTick(bufferId).also {
                (it + 1..it + beforeDelete + afterDelete + 1).forEach { c ->
                    ignoreChangedTicks.add(c)
                }
            }
            if (beforeDelete > 0 || afterDelete > 0) {
                client.sendDeletion(beforeDelete, afterDelete)
            }
            client.paste(text)
        }
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

        sendChan.trySend {
            client.changedTick(bufferId)
                .also { ignoreChangedTicks.add(it + 1) }
            client.bufferSetText(bufferId, start, end, replacement)
        }
    }

    override fun dispose() {
        documentListenerGuard.unregister()
    }
}
