package com.ugarosa.neovim.adapter.idea.ui.cmdline

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.awt.RelativePoint
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.domain.mode.getMode
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.Point

@Service(Service.Level.PROJECT)
class NvimCmdlineManager(
    private val project: Project,
    private val scope: CoroutineScope,
) {
    private val logger = myLogger()
    private val client = service<NvimClient>()

    private var popup: JBPopup? = null

    suspend fun handleEvent(event: CmdlineEvent) {
        val editor =
            focusEditor() ?: run {
                logger.warn("No focused editor found, cannot handle CmdlineEvent: $event")
                destroy()
                return
            }

        logger.trace("Handling CmdlineEvent: $event")
        withContext(Dispatchers.EDT) {
            val view = project.service<CmdlineView>()
            when (event) {
                is CmdlineEvent.Show -> view.updateModel(show = event)
                is CmdlineEvent.Pos -> view.updateModel(pos = event.pos)
                is CmdlineEvent.SpecialChar -> view.updateModel(specialChar = event.c)
                is CmdlineEvent.Hide -> view.clearSingle()
                is CmdlineEvent.BlockShow -> view.updateModel(blockShow = event.lines)
                is CmdlineEvent.BlockAppend -> view.updateModel(blockAppend = event.line)
                is CmdlineEvent.BlockHide -> view.clearBlock()
                is CmdlineEvent.Flush -> {
                    // No change in model
                    if (!view.flush()) return@withContext

                    if (view.isHidden()) {
                        logger.trace("Cmdline is hidden, not showing popup: $event")
                        destroy()
                    } else if (popup == null || popup!!.isDisposed) {
                        logger.trace("Cmdline is shown, creating popup: $event")
                        showPopup(editor, view)
                    } else {
                        logger.trace("Cmdline is shown, updating popup: $event")
                        resize(editor, view)
                    }
                }
            }
        }
    }

    private suspend fun destroy() =
        withContext(Dispatchers.EDT) {
            popup?.cancel()
            popup = null
        }

    private fun showPopup(
        editor: Editor,
        view: CmdlineView,
    ) {
        val (loc, size) = bottomLocationAndSize(editor, view)
        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(view.component, null)
                .setResizable(true)
                .setMovable(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .setShowBorder(false)
                .setShowShadow(false)
                .setMinSize(size)
                .addListener(PopupCloseListener(scope, client))
                .createPopup()
                .apply {
                    this.size = size
                    show(RelativePoint(editor.component, loc))
                }
    }

    private class PopupCloseListener(
        private val scope: CoroutineScope,
        private val client: NvimClient,
    ) : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            scope.launch {
                if (getMode().isCommand()) {
                    client.input("<Esc>")
                }
            }
        }
    }

    private fun resize(
        editor: Editor,
        view: CmdlineView,
    ) {
        val (loc, size) = bottomLocationAndSize(editor, view)
        popup?.apply {
            this.size = size
            this.setLocation(RelativePoint(editor.component, loc).screenPoint)
        }
    }

    private fun bottomLocationAndSize(
        editor: Editor,
        view: CmdlineView,
    ): Pair<Point, Dimension> {
        val width = editor.component.width
        val height = view.getHeight()

        val x = 0
        val y = editor.component.height - height

        return Point(x, y) to Dimension(width, height)
    }
}
