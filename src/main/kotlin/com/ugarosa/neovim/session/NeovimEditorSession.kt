package com.ugarosa.neovim.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.wm.WindowManager
import com.ugarosa.neovim.common.utf8ByteOffsetToCharOffset
import com.ugarosa.neovim.factory.NEOVIM_MODE_ID
import com.ugarosa.neovim.factory.NeovimModeWidget
import com.ugarosa.neovim.infra.NeovimRpcClient
import com.ugarosa.neovim.service.BufLinesEvent
import com.ugarosa.neovim.service.BufferId
import com.ugarosa.neovim.service.NeovimFunctions
import com.ugarosa.neovim.service.NeovimMode

class NeovimEditorSession(
    private val rpcClient: NeovimRpcClient,
    private val editor: Editor,
) {
    private val bufferId: BufferId = NeovimFunctions.createBuffer(rpcClient)

    init {
        rpcClient.registerPushHandler { push ->
            val event = NeovimFunctions.maybeBufLinesEvent(push)
            if (event?.bufferId == bufferId) {
                handleBufferLinesEvent(event)
            }
        }

        initializeBuffer()
        attachBuffer()

        editor.putUserData(NEOVIM_SESSION_KEY, this)
    }

    fun activateBuffer() {
        NeovimFunctions.setCurrentBuffer(rpcClient, bufferId)
    }

    fun sendKeyAndFetchStatus(key: String) {
        NeovimFunctions.input(rpcClient, key)
        val pos = getCursor()
        editor.caretModel.moveToLogicalPosition(pos)
        updateModeWidget()
    }

    private fun initializeBuffer() {
        val lines = editor.document.text.split("\n")
        NeovimFunctions.bufferSetLines(rpcClient, bufferId, 0, -1, lines)
    }

    private fun attachBuffer() {
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

            val (row, col) = getCursor().run { line to column }
            editor.caretModel.moveToLogicalPosition(LogicalPosition(row, col))
        }
    }

    private fun getCursor(): LogicalPosition {
        val document = editor.document
        // Neovim uses (1, 0) byte-based indexing
        val (nvimRow, nvimByteCol) = NeovimFunctions.getCursor(rpcClient)
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

    private fun updateModeWidget() {
        val mode = NeovimFunctions.getMode(rpcClient)
        val project = editor.project ?: return
        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(NEOVIM_MODE_ID)
        if (widget is NeovimModeWidget) {
            widget.updateMode(mode)
        }
        applyCursorShape(mode)
    }

    private fun applyCursorShape(mode: NeovimMode) {
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
