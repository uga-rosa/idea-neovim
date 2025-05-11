package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.Point

@Service(Service.Level.APP)
class NeovimCmdlinePopupImpl() : NeovimCmdlinePopup {
    private val logger = thisLogger()

    private var editor: Editor? = null
    private var popup: JBPopup? = null
    private val panel = CmdlinePanel()

    override fun attachTo(editor: Editor) {
        this.editor = editor
    }

    override suspend fun handleEvent(event: CmdlineEvent) {
        logger.trace("Handling CmdlineEvent: $event")
        withContext(Dispatchers.EDT) {
            when (event) {
                is CmdlineEvent.Show -> panel.updateModel(show = event)
                is CmdlineEvent.Pos -> panel.updateModel(pos = event.pos)
                is CmdlineEvent.SpecialChar -> panel.updateModel(specialChar = event.c)
                is CmdlineEvent.Hide -> panel.clear()
                is CmdlineEvent.BlockShow -> panel.updateModel(blockShow = event.lines)
                is CmdlineEvent.BlockAppend -> panel.updateModel(blockAppend = event.line)
                is CmdlineEvent.BlockHide -> panel.updateModel(blockHide = true)
                is CmdlineEvent.Flush -> {
                    if (panel.isHidden()) {
                        logger.debug("Cmdline is hidden, not showing popup")
                        destroy()
                    } else if (popup == null || popup!!.isDisposed) {
                        logger.debug("Cmdline is shown, creating popup")
                        showPopup()
                    } else {
                        logger.debug("Cmdline is shown, updating popup")
                        panel.flush()
                        resize()
                    }
                }
            }
        }
    }

    private fun showPopup() {
        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setResizable(true)
                .setMovable(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .createPopup()

        val (loc, size) = centerLocationAndSize()
        popup?.show(RelativePoint(editor!!.component, loc))
        popup?.size = size
    }

    private fun resize() {
        popup?.size = panel.preferredSize
        val (loc, size) = centerLocationAndSize()
        popup?.setSize(loc, size)
    }

    private fun centerLocationAndSize(): Pair<Point, Dimension> {
        val component = editor?.component ?: error("Editor didn't attached")
        val width = (component.width * 0.8).toInt()
        val height = panel.preferredSize.height
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
