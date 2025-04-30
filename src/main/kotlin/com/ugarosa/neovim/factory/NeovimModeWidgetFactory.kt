package com.ugarosa.neovim.factory

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ugarosa.neovim.rpc.NeovimMode
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
    private var mode: String = NeovimMode.NORMAL.name
    private var statusBar: StatusBar? = null
    private val label =
        JLabel().apply {
            text = mode
            border = JBUI.Borders.empty(0, 6)
            isOpaque = true
            foreground = JBColor.foreground()
            background = NeovimMode.NORMAL.color
        }

    override fun ID(): String = NEOVIM_MODE_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {}

    override fun getComponent() = label

    fun updateMode(newMode: NeovimMode) {
        if (mode != newMode.name) {
            mode = newMode.name
            label.text = newMode.name
            label.background = newMode.color
            statusBar?.updateWidget(ID())
        }
    }
}
