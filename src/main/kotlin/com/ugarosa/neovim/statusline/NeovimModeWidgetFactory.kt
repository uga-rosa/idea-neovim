package com.ugarosa.neovim.statusline

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.mode.NeovimModeKind
import com.ugarosa.neovim.mode.NeovimModeManager
import javax.swing.JLabel

const val NEOVIM_MODE_ID = "NeovimModeWidgetId"

class NeovimModeWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = NEOVIM_MODE_ID

    override fun getDisplayName(): String = "Neovim Mode Display"

    override fun isAvailable(project: Project): Boolean = true

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return NeovimModeWidget()
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }
}

class NeovimModeWidget : CustomStatusBarWidget {
    init {
        service<NeovimModeManager>().addHook { _, new ->
            updateMode(new)
        }
    }

    private var mode: NeovimMode = NeovimMode.default
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

    fun updateMode(newMode: NeovimMode) {
        if (mode != newMode) {
            mode = newMode
            label.text = newMode.kind.name
            label.background = modeToColor(newMode)
            statusBar?.updateWidget(ID())
        }
    }

    private fun modeToColor(mode: NeovimMode): JBColor {
        return when (mode.kind) {
            NeovimModeKind.NORMAL -> JBColor.GREEN

            NeovimModeKind.VISUAL,
            NeovimModeKind.SELECT,
            -> JBColor.BLUE

            NeovimModeKind.INSERT -> JBColor.YELLOW
            NeovimModeKind.REPLACE -> JBColor.ORANGE
            NeovimModeKind.COMMAND -> JBColor.GREEN
        }
    }
}
