package com.ugarosa.neovim.document

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.TextRange
import com.ugarosa.neovim.common.GroupIdGenerator
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.charOffsetToUtf8ByteOffset
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.BufLinesEvent
import com.ugarosa.neovim.rpc.function.BufferSetTextParams
import com.ugarosa.neovim.rpc.function.bufferAttach
import com.ugarosa.neovim.rpc.function.bufferDetach
import com.ugarosa.neovim.rpc.function.bufferSetLines
import com.ugarosa.neovim.rpc.function.bufferSetText
import com.ugarosa.neovim.rpc.function.input
import com.ugarosa.neovim.rpc.function.setCurrentBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeovimDocumentHandler private constructor(
    private val scope: CoroutineScope,
    private val editor: Editor,
    private val bufferId: BufferId,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val documentListenerGuard =
        ListenerGuard(
            NeovimDocumentListener(this),
            { editor.document.addDocumentListener(it) },
            { editor.document.removeDocumentListener(it) },
        ).apply {
            logger.trace("Registering document listener for buffer: $bufferId")
            register()
        }

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            bufferId: BufferId,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(scope, editor, bufferId)
            handler.initializeBuffer()
            handler.enableBufLinesEvent()
            return handler
        }
    }

    private suspend fun initializeBuffer() {
        val liens = editor.document.text.split("\n")
        bufferSetLines(client, bufferId, 0, -1, liens)
            .onRight { logger.trace("Initialized buffer: $bufferId") }
            .onLeft { logger.warn("Failed to initialize: $it") }
    }

    suspend fun enableBufLinesEvent() {
        bufferAttach(client, bufferId)
            .onRight { logger.trace("Attached buffer: $bufferId") }
            .onLeft { logger.warn("Failed to attach buffer: $it") }
    }

    suspend fun disableBufLinesEvent() {
        bufferDetach(client, bufferId)
            .onRight { logger.trace("Detached buffer: $bufferId") }
            .onLeft { logger.warn("Failed to detach buffer: $it") }
    }

    suspend fun activateBuffer() {
        setCurrentBuffer(client, bufferId)
            .onRight { logger.trace("Set current buffer: $bufferId") }
            .onLeft { logger.warn("Failed to set current buffer to window: $it") }
    }

    fun applyBufferLinesEvent(e: BufLinesEvent) {
        val document = editor.document
        val startOffset = document.getLineStartOffset(e.firstLine)
        val endOffset =
            if (e.lastLine == -1) {
                document.textLength
            } else {
                document.getLineStartOffset(e.lastLine)
            }
        val replacementText =
            if (e.replacementLines.isEmpty()) {
                ""
            } else {
                e.replacementLines.joinToString("\n", postfix = "\n")
            }
        scope.launch(Dispatchers.EDT) {
            WriteCommandAction.runWriteCommandAction(
                editor.project,
                "ApplyBufLinesEvent",
                GroupIdGenerator.generate(),
                {
                    documentListenerGuard.runWithoutListener {
                        document.replaceString(startOffset, endOffset, replacementText)
                        logger.trace("Applied buffer lines event: $e")
                    }
                },
            )
        }
    }

    // Must be called before the document is changed
    fun syncDocumentChange(event: DocumentEvent) {
        if (isSingleLineChange(event)) {
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
            input(client, deleteStr + insertStr)
        }
    }

    // Apply multiline text change
    private fun sendBufferSetText(event: DocumentEvent) {
        val document = event.document

        val startOffset = event.offset
        val startRow = document.getLineNumber(startOffset)
        val startLineStartOffset = document.getLineStartOffset(startRow)
        val startCharCol = startOffset - startLineStartOffset
        val startLineText = document.getText(TextRange(startLineStartOffset, document.getLineEndOffset(startRow)))
        val startByteCol = charOffsetToUtf8ByteOffset(startLineText, startCharCol)

        val endRow = document.getLineNumber(startOffset + event.oldLength)
        val endLineStartOffset = document.getLineStartOffset(endRow)
        val endCharCol = startOffset + event.oldLength - endLineStartOffset
        val endLineText = document.getText(TextRange(endLineStartOffset, document.getLineEndOffset(endRow)))
        val endByteCol = charOffsetToUtf8ByteOffset(endLineText, endCharCol)

        val replacement =
            event.newFragment.toString()
                .replace("\r\n", "\n")
                .split("\n")

        val params = BufferSetTextParams(bufferId, startRow, startByteCol, endRow, endByteCol, replacement)
        scope.launch {
            bufferSetText(client, params)
                .onRight { logger.trace("Sync document change: $params") }
                .onLeft { logger.warn("Failed to sync document change: $it") }
        }
    }
}
