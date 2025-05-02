package com.ugarosa.neovim.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.rpc.BufLinesEvent
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.NeovimFunctions
import com.ugarosa.neovim.rpc.NeovimRpcClient

class NeovimDocumentHandler private constructor(
    private val rpcClient: NeovimRpcClient,
    private val editor: Editor,
    private val bufferId: BufferId,
) {
    companion object {
        suspend fun create(
            rpcClient: NeovimRpcClient,
            editor: Editor,
            bufferId: BufferId,
        ): NeovimDocumentHandler {
            val handler = NeovimDocumentHandler(rpcClient, editor, bufferId)
            handler.initializeBuffer()
            handler.attachBuffer()
            return handler
        }
    }

    private suspend fun initializeBuffer() {
        val liens = editor.document.text.split("\n")
        NeovimFunctions.bufferSetLines(rpcClient, bufferId, 0, -1, liens)
    }

    private suspend fun attachBuffer() {
        NeovimFunctions.bufferAttach(rpcClient, bufferId)
    }

    suspend fun detachBuffer() {
        NeovimFunctions.bufferDetach(rpcClient, bufferId)
    }

    suspend fun activateBuffer() {
        NeovimFunctions.setCurrentBuffer(rpcClient, bufferId)
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
