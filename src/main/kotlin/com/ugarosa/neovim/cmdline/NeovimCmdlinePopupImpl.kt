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
    private val client = getClient()

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
                is CmdlineEvent.Show -> panel.showCmdline(event)
                is CmdlineEvent.Pos -> panel.updateCursor(event.pos)
                is CmdlineEvent.SpecialChar -> panel.showSpecialChar(event.c, event.shift)
                is CmdlineEvent.Hide -> {
                    hide()
                    return@withContext
                }

                is CmdlineEvent.BlockShow -> panel.showBlock(event.lines)
                is CmdlineEvent.BlockAppend -> panel.appendBlockLine(event.line)
                is CmdlineEvent.BlockHide -> panel.hideBlock()
            }

            if (popup == null || popup!!.isDisposed) {
                showPopup()
            } else {
                panel.repaint()
            }
        }
    }

    private fun showPopup() {
        val component = editor?.component ?: return
        val size = component.size
        val popupWidth = (size.width * 0.8).toInt()
        val popupHeight = panel.preferredSize.height

        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setResizable(false)
                .setMovable(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .setMinSize(Dimension(popupWidth, popupHeight))
                .createPopup()
                .also { jBPopup ->
                    jBPopup.addListener(
                        object : JBPopupListener {
                            override fun onClosed(event: LightweightWindowEvent) {
                                scope.launch { input(client, "<Esc>") }
                            }
                        },
                    )
                }

        val x = (size.width - popupWidth) / 2
        val y = (size.height - popupHeight) / 2
        popup?.show(RelativePoint(component, Point(x, y)))
    }

    private fun hide() {
        logger.debug("Hiding CmdlinePopup")
        popup?.cancel()
        popup = null
        panel.clear()
    }
}
