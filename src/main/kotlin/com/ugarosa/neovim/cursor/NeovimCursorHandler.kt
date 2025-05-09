package com.ugarosa.neovim.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.ugarosa.neovim.common.ListenerGuard
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.getOptionManager
import com.ugarosa.neovim.common.toLogicalPosition
import com.ugarosa.neovim.common.toNeovimPosition
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.CursorMoveEvent
import com.ugarosa.neovim.rpc.function.setCursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeovimCursorHandler private constructor(
    scope: CoroutineScope,
    private val editor: Editor,
    private val disposable: Disposable,
    private val bufferId: BufferId,
) {
    private val logger = thisLogger()
    private val client = getClient()
    private val optionManager = getOptionManager()
    private val caretListenerGuard =
        ListenerGuard(
            NeovimCaretListener(scope, this),
            { editor.caretModel.addCaretListener(it, disposable) },
            { editor.caretModel.removeCaretListener(it) },
        )

    companion object {
        suspend fun create(
            scope: CoroutineScope,
            editor: Editor,
            disposable: Disposable,
            bufferId: BufferId,
        ): NeovimCursorHandler {
            val handler = NeovimCursorHandler(scope, editor, disposable, bufferId)
            withContext(Dispatchers.EDT) {
                editor.settings.isBlockCursor = true
            }
            handler.enableCursorListener()
            return handler
        }
    }

    fun enableCursorListener() {
        logger.trace("Enabling cursor listener for buffer: $bufferId")
        caretListenerGuard.register()
    }

    fun disableCursorListener() {
        logger.trace("Disabling cursor listener for buffer: $bufferId")
        caretListenerGuard.unregister()
    }

    suspend fun syncNeovimToIdea(event: CursorMoveEvent) {
        val pos = event.position.toLogicalPosition(editor.document)
        caretListenerGuard.runWithoutListenerSuspend {
            withContext(Dispatchers.EDT) {
                editor.caretModel.moveToLogicalPosition(pos)
                val (scrolloff, sidescrolloff) = getScrollOptions()
                scrollLineIntoView(pos.line, scrolloff)
                scrollColumnIntoView(pos.column, sidescrolloff)
            }
        }
    }

    private suspend fun getScrollOptions(): Pair<Scrolloff, Sidescrolloff> {
        val options = optionManager.getLocal(bufferId)
        return options.scrolloff to options.sidescrolloff
    }

    private fun scrollLineIntoView(
        line: Int,
        scrolloff: Scrolloff,
    ) {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea
        val lineHeight = editor.lineHeight

        val firstVisibleLine = editor.yToVisualLine(visibleArea.y)
        val lastVisibleLine = editor.yToVisualLine(visibleArea.y + visibleArea.height)

        val targetTop = line - scrolloff.value
        val targetBottom = line + scrolloff.value

        when {
            targetTop < firstVisibleLine -> {
                val scrollToY = targetTop.coerceAtLeast(0) * lineHeight
                scrollingModel.scrollVertically(scrollToY)
            }

            targetBottom > lastVisibleLine -> {
                val linesToScroll = targetBottom - lastVisibleLine
                scrollingModel.scrollVertically(visibleArea.y + linesToScroll * lineHeight)
            }
            // already in view
        }
    }

    private val charWidth: Int by lazy {
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        editor.contentComponent.getFontMetrics(font)
            .charWidth('W')
    }

    private fun scrollColumnIntoView(
        column: Int,
        sidescrolloff: Sidescrolloff,
    ) {
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea

        val firstVisibleColumn = visibleArea.x / charWidth
        val lastVisibleColumn = (visibleArea.x + visibleArea.width) / charWidth

        val targetLeft = column - sidescrolloff.value
        val targetRight = column + sidescrolloff.value

        when {
            targetLeft < firstVisibleColumn -> {
                val scrollToX = (targetLeft.coerceAtLeast(0) * charWidth)
                scrollingModel.scrollHorizontally(scrollToX)
            }

            targetRight > lastVisibleColumn -> {
                val colsToScroll = targetRight - lastVisibleColumn
                scrollingModel.scrollHorizontally(visibleArea.x + colsToScroll * charWidth)
            }
            // already in view
        }
    }

    suspend fun syncIdeaToNeovim() {
        val logicalPosition = editor.caretModel.logicalPosition
        val neovimPosition = logicalPosition.toNeovimPosition(editor.document)
        setCursor(client, neovimPosition.row, neovimPosition.col)
    }

    suspend fun changeCursorShape(mode: NeovimMode) {
        withContext(Dispatchers.EDT) {
            val option = optionManager.getLocal(bufferId)
            editor.settings.isBlockCursor = mode.isBlock(option.selection)
        }
    }
}
