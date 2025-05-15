package com.ugarosa.neovim.session.document

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.client.api.activateBuffer
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
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NeovimDocumentHandler private constructor(
    private val scope: CoroutineScope,
    private val editor: Editor,
    private val bufferId: BufferId,
) {
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
            scope: CoroutineScope,
            editor: Editor,
            bufferId: BufferId,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(scope, editor, bufferId)
            handler.initializeBuffer()
            handler.enableListener()
            return handler
        }
    }

    private suspend fun initializeBuffer() {
        val liens = editor.document.text.split("\n")
        client.bufferSetLines(bufferId, 0, -1, liens)

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

    suspend fun activateBuffer() {
        client.activateBuffer(bufferId)
    }

    fun applyBufferLinesEvent(e: BufLinesEvent) {
        if (getMode().isInsert()) {
            logger.trace("Ignore event in insert mode: $e")
            return
        }
        if (ignoreChangedTicks.remove(e.changedTick)) {
            logger.trace("Ignore event by changed tick: $e")
            return
        }

        val document = editor.document
        val (startOffset, endOffset) =
            runReadAction {
                val start = document.getLineStartOffset(e.firstLine)
                if (e.lastLine == -1 || e.lastLine >= document.lineCount) {
                    // To successfully delete the line when the last line does not have a trailing newline character,
                    // you need to remove the newline of the previous line.
                    val end = document.textLength
                    start - 1 to end
                } else {
                    val end = document.getLineStartOffset(e.lastLine)
                    start to end
                }
            }
        val replacementText =
            if (e.replacementLines.isEmpty()) {
                ""
            } else {
                e.replacementLines.joinToString("\n", postfix = "\n")
            }
        documentListenerGuard.runWithoutListener {
            WriteCommandAction.writeCommandAction(editor.project)
                .run<Exception> {
                    document.replaceString(startOffset, endOffset, replacementText)
                }
            logger.trace("Applied buffer lines event: $e")
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

        scope.launch {
            client.changedTick(bufferId)
                .also { ignoreChangedTicks.add(it + 1) }
            client.input(deleteStr + insertStr)
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

        scope.launch {
            client.changedTick(bufferId)
                .also { ignoreChangedTicks.add(it + 1) }
            client.bufferSetText(bufferId, start, end, replacement)
        }
    }
}
