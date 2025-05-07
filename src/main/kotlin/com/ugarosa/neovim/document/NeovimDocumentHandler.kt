package com.ugarosa.neovim.document

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.TextRange
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
import com.ugarosa.neovim.rpc.function.setCurrentBuffer
import kotlinx.coroutines.CoroutineScope
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
        ApplicationManager.getApplication().invokeLater {
            documentListenerGuard.runWithoutListener {
                WriteCommandAction.runWriteCommandAction(editor.project) {
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
                    document.replaceString(startOffset, endOffset, replacementText)
                    logger.trace("Applied buffer lines event: $e")
                }
            }
        }
    }

    // Must be called before the document is changed
    fun syncDocumentChange(event: DocumentEvent) {
        // IDEA uses (0, 0) character offset indexing
        // :h nvim_buf_set_text()
        //   Indexing is zero-based. Row indices are end-inclusive, and column indices are end-exclusive.

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
