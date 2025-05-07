package com.ugarosa.neovim.statusline

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.ugarosa.neovim.mode.NeovimMode

class StatusLineHandler(
    private val project: Project,
) {
    private val logger = thisLogger()

    fun updateStatusLine(mode: NeovimMode) {
        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(NEOVIM_MODE_ID)
        if (widget !is NeovimModeWidget) {
            logger.warn("NeovimModeWidget not found in status bar")
            return
        }
        widget.updateMode(mode)
    }
}
