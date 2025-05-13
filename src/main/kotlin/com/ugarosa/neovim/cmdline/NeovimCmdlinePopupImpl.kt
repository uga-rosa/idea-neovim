package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.awt.RelativePoint
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.mode.getMode
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent
import com.ugarosa.neovim.rpc.function.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.Point

@Service(Service.Level.APP)
class NeovimCmdlinePopupImpl(
    private val scope: CoroutineScope,
) : NeovimCmdlinePopup {
    private val logger = myLogger()
    private val client = getClient()

    private var popup: JBPopup? = null
    private val pane = CmdlinePane()

    override suspend fun handleEvent(event: CmdlineEvent) {
        val editor =
            focusEditor() ?: run {
                logger.warn("No focused editor found, cannot handle CmdlineEvent: $event")
                destroy()
                return
            }

        logger.trace("Handling CmdlineEvent: $event")
        withContext(Dispatchers.EDT) {
            when (event) {
                is CmdlineEvent.Show -> pane.updateModel(show = event)
                is CmdlineEvent.Pos -> pane.updateModel(pos = event.pos)
                is CmdlineEvent.SpecialChar -> pane.updateModel(specialChar = event.c)
                is CmdlineEvent.Hide -> pane.clearSingle()
                is CmdlineEvent.BlockShow -> pane.updateModel(blockShow = event.lines)
                is CmdlineEvent.BlockAppend -> pane.updateModel(blockAppend = event.line)
                is CmdlineEvent.BlockHide -> pane.clearBlock()
                is CmdlineEvent.Flush -> {
                    if (pane.isHidden()) {
                        logger.trace("Cmdline is hidden, not showing popup: $event")
                        destroy()
                    } else {
                        pane.flush()
                        if (popup == null || popup!!.isDisposed) {
                            logger.trace("Cmdline is shown, creating popup: $event")
                            showPopup(editor)
                        } else {
                            logger.trace("Cmdline is shown, updating popup: $event")
                            resize(editor)
                        }
                    }
                }
            }
        }
    }

    private fun showPopup(editor: Editor) {
        val (loc, size) = centerLocationAndSize(editor)
        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(pane, null)
                .setResizable(true)
                .setMovable(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .setMinSize(size)
                .createPopup()
                .apply {
                    addListener(PopupCloseListener(scope, client))
                }

        popup?.show(RelativePoint(editor.component, loc))
    }

    private class PopupCloseListener(
        private val scope: CoroutineScope,
        private val client: NeovimRpcClient,
    ) : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            scope.launch {
                if (getMode().isCommand()) {
                    input(client, "<Esc>")
                }
            }
        }
    }

    private fun resize(editor: Editor) {
        popup?.size = pane.preferredSize
        val (_, size) = centerLocationAndSize(editor)
        popup?.size = size
    }

    private fun centerLocationAndSize(editor: Editor): Pair<Point, Dimension> {
        val component = editor.component
        val pref = pane.preferredSize
        val width = (component.width * 0.8).toInt().coerceAtLeast(pref.width)
        val height = pref.height
        val x = (component.width - width) / 2
        val y = (component.height - height) / 2
        return Point(x, y) to Dimension(width, height)
    }

    override suspend fun destroy() =
        withContext(Dispatchers.EDT) {
            pane.reset()
            popup?.cancel()
            popup = null
        }
}
