package com.ugarosa.neovim.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.wm.WindowManager
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.factory.NEOVIM_MODE_ID
import com.ugarosa.neovim.factory.NeovimModeWidget
import com.ugarosa.neovim.infra.NeovimRpcClient
import com.ugarosa.neovim.rpc.BufLinesEvent
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.NeovimFunctions
import com.ugarosa.neovim.rpc.NeovimMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NeovimEditorSession(
    private val rpcClient: NeovimRpcClient,
    private val editor: Editor,
    private val scope: CoroutineScope,
) {
    private lateinit var bufferId: BufferId

    init {
        scope.launch(Dispatchers.IO) {
            bufferId = NeovimFunctions.createBuffer(rpcClient)

            rpcClient.registerPushHandler { push ->
                val event = NeovimFunctions.maybeBufLinesEvent(push)
                if (event?.bufferId == bufferId) {
                    handleBufferLinesEvent(event)
                }
            }

            initializeBuffer()
            attachBuffer()
        }
    }

    fun activateBuffer() {
        scope.launch(Dispatchers.IO) {
            NeovimFunctions.setCurrentBuffer(rpcClient, bufferId)
        }
    }

    fun sendKeyAndSyncStatus(key: String) {
        scope.launch(Dispatchers.IO) {
            NeovimFunctions.input(rpcClient, key)
            syncCursorFromNeovimToIdea()
            syncNeovimMode()
        }
    }

    private suspend fun initializeBuffer() {
        val lines = editor.document.text.split("\n")
        NeovimFunctions.bufferSetLines(rpcClient, bufferId, 0, -1, lines)
    }

    private suspend fun attachBuffer() {
        NeovimFunctions.bufferAttach(rpcClient, bufferId)
    }

    private fun handleBufferLinesEvent(e: BufLinesEvent) {
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

            scope.launch(Dispatchers.IO) {
                syncCursorFromNeovimToIdea()
            }
        }
    }

    private suspend fun syncCursorFromNeovimToIdea() {
        val (row, col) = getNeovimCursor().run { line to column }
        withContext(Dispatchers.EDT) {
            editor.caretModel.moveToLogicalPosition(LogicalPosition(row, col))
        }
    }

    private suspend fun getNeovimCursor(): LogicalPosition {
        // Neovim uses (1, 0) byte-based indexing
        val (nvimRow, nvimByteCol) = NeovimFunctions.getCursor(rpcClient)
        val document = editor.document
        // IntelliJ uses 0-based line indexing
        val lineIndex = nvimRow - 1
        if (lineIndex < 0 || lineIndex >= document.lineCount) {
            return LogicalPosition(0, 0)
        }

        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val lineText = document.text.substring(lineStartOffset, lineEndOffset)
        val correctedCol = utf8ByteOffsetToCharOffset(lineText, nvimByteCol)

        return LogicalPosition(lineIndex, correctedCol)
    }

    private suspend fun syncNeovimMode() {
        val mode = NeovimFunctions.getMode(rpcClient)
        updateModeWidget(mode)
        applyCursorShape(mode)
    }

    private suspend fun updateModeWidget(mode: NeovimMode) {
        val project = editor.project ?: return
        withContext(Dispatchers.EDT) {
            val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(NEOVIM_MODE_ID)
            check(widget is NeovimModeWidget) { "NeovimModeWidget not found in status bar" }
            widget.updateMode(mode)
        }
    }

    private suspend fun applyCursorShape(mode: NeovimMode) {
        withContext(Dispatchers.EDT) {
            editor.settings.isBlockCursor = mode in
                setOf(
                    NeovimMode.NORMAL,
                    NeovimMode.VISUAL,
                    NeovimMode.VISUAL_LINE,
                    NeovimMode.VISUAL_BLOCK,
                    NeovimMode.SELECT,
                )
        }
    }
}
