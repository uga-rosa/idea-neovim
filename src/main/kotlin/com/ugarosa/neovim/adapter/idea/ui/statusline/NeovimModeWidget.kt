package com.ugarosa.neovim.adapter.idea.ui.statusline

import com.intellij.openapi.components.service
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ugarosa.neovim.domain.mode.NeovimModeManager
import com.ugarosa.neovim.domain.mode.NvimMode
import com.ugarosa.neovim.domain.mode.NvimModeKind
import javax.swing.JLabel

class NeovimModeWidget : CustomStatusBarWidget {
    init {
        service<NeovimModeManager>().addHook { _, new ->
            updateMode(new)
        }
    }

    private var mode: NvimMode = NvimMode.default
    private var statusBar: StatusBar? = null
    private val label =
        JLabel().apply {
            border = JBUI.Borders.empty(0, 6)
            isOpaque = true
            text = mode.kind.name
            foreground = JBColor.foreground()
            background = modeToColor(mode)
        }

    override fun ID(): String = NEOVIM_MODE_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {}

    override fun getComponent() = label

    fun updateMode(newMode: NvimMode) {
        if (mode != newMode) {
            mode = newMode
            label.text = newMode.kind.name
            label.background = modeToColor(newMode)
            statusBar?.updateWidget(ID())
        }
    }

    private fun modeToColor(mode: NvimMode): JBColor {
        return when (mode.kind) {
            NvimModeKind.NORMAL -> JBColor.GREEN

            NvimModeKind.VISUAL,
            NvimModeKind.SELECT,
            -> JBColor.BLUE

            NvimModeKind.INSERT -> JBColor.YELLOW
            NvimModeKind.REPLACE -> JBColor.ORANGE
            NvimModeKind.COMMAND -> JBColor.GREEN
        }
    }
}
