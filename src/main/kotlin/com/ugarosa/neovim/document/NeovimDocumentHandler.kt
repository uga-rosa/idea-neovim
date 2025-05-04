package com.ugarosa.neovim.document

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.BufLinesEvent
import com.ugarosa.neovim.rpc.function.bufferAttach
import com.ugarosa.neovim.rpc.function.bufferDetach
import com.ugarosa.neovim.rpc.function.bufferSetLines
import com.ugarosa.neovim.rpc.function.setCurrentBuffer

class NeovimDocumentHandler private constructor(
    private val client: NeovimRpcClient,
    private val editor: Editor,
    private val bufferId: BufferId,
) {
    private val logger = thisLogger()

    companion object {
        suspend fun create(
            client: NeovimRpcClient,
            editor: Editor,
            bufferId: BufferId,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(client, editor, bufferId)
            handler.initializeBuffer()
            handler.attachBuffer()
            return handler
        }
    }

    private suspend fun initializeBuffer() {
        val liens = editor.document.text.split("\n")
        bufferSetLines(client, bufferId, 0, -1, liens)
            .onLeft { logger.warn("Failed to initialize: $it") }
    }

    private suspend fun attachBuffer() {
        bufferAttach(client, bufferId)
            .onLeft { logger.warn("Failed to attach buffer: $it") }
    }

    suspend fun detachBuffer() {
        bufferDetach(client, bufferId)
            .onLeft { logger.warn("Failed to detach buffer: $it") }
    }

    suspend fun activateBuffer() {
        setCurrentBuffer(client, bufferId)
            .onLeft { logger.warn("Failed to set current buffer to window") }
    }

    fun applyBufferLinesEvent(e: BufLinesEvent) {
        ApplicationManager.getApplication().invokeLater {
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
            }
        }
    }
}
