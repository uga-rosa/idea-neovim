package com.ugarosa.neovim.statusline

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.ugarosa.neovim.logger.myLogger

@Service(Service.Level.PROJECT)
class StatusLineManager(
    private val project: Project,
) {
    private val logger = myLogger()

    fun updateStatusLine() {
        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(NEOVIM_MODE_ID)
        if (widget !is NeovimModeWidget) {
            logger.warn("NeovimModeWidget not found in status bar")
            return
        }
        widget.updateMode()
    }
}
