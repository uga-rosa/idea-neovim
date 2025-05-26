package com.ugarosa.neovim.adapter.idea.ui.statusline

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

const val NEOVIM_MODE_ID = "NeovimModeWidgetId"

class NeovimModeWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = NEOVIM_MODE_ID

    override fun getDisplayName(): String = "Neovim Mode Display"

    override fun isAvailable(project: Project): Boolean = true

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return NvimModeWidget()
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }
}
