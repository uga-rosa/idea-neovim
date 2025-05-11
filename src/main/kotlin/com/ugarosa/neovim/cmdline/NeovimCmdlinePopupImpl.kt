package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.awt.RelativePoint
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.mode.getMode
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
    private val logger = thisLogger()

    private var editor: Editor? = null
    private var popup: JBPopup? = null
    private val panel = CmdlinePanel()

    override fun attachTo(editor: Editor) {
        this.editor = editor
    }

    override suspend fun handleEvent(event: CmdlineEvent) {
        logger.debug("Handling CmdlineEvent: $event")
        withContext(Dispatchers.EDT) {
            when (event) {
                is CmdlineEvent.Show -> panel.updateModel(show = event)
                is CmdlineEvent.Pos -> panel.updateModel(pos = event.pos)
                is CmdlineEvent.SpecialChar -> panel.updateModel(specialChar = event.c)
                is CmdlineEvent.Hide -> panel.clearSingle()
                is CmdlineEvent.BlockShow -> panel.updateModel(blockShow = event.lines)
                is CmdlineEvent.BlockAppend -> panel.updateModel(blockAppend = event.line)
                is CmdlineEvent.BlockHide -> panel.clearBlock()
                is CmdlineEvent.Flush -> {
                    if (panel.isHidden()) {
                        logger.trace("Cmdline is hidden, not showing popup: $event")
                        destroy()
                    } else if (popup == null || popup!!.isDisposed) {
                        logger.trace("Cmdline is shown, creating popup: $event")
                        showPopup()
                    } else {
                        logger.trace("Cmdline is shown, updating popup: $event")
                        panel.flush()
                        resize()
                    }
                }
            }
        }
    }

    private fun showPopup() {
        val (loc, size) = centerLocationAndSize()

        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setResizable(true)
                .setMovable(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .setMinSize(size)
                .createPopup()
                .apply {
                    addListener(PopupCloseListener(scope))
                }

        popup?.show(RelativePoint(editor!!.component, loc))
    }

    private class PopupCloseListener(
        private val scope: CoroutineScope,
    ) : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            scope.launch {
                if (getMode().isCommand()) {
                    input(getClient(), "<Esc>")
                }
            }
        }
    }

    private fun resize() {
        popup?.size = panel.preferredSize
        val (_, size) = centerLocationAndSize()
        popup?.size = size
    }

    private fun centerLocationAndSize(): Pair<Point, Dimension> {
        val component = editor?.component ?: error("Editor didn't attached")
        val pref = panel.preferredSize
        val width = (component.width * 0.8).toInt().coerceAtLeast(pref.width)
        val height = pref.height
        val x = (component.width - width) / 2
        val y = (component.height - height) / 2
        return Point(x, y) to Dimension(width, height)
    }

    override suspend fun destroy() =
        withContext(Dispatchers.EDT) {
            popup?.cancel()
            popup = null
        }
}
