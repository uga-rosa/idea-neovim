package com.ugarosa.neovim.session

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.ugarosa.neovim.factory.NEOVIM_MODE_ID
import com.ugarosa.neovim.factory.NeovimModeWidget
import com.ugarosa.neovim.rpc.NeovimMode

class StatusLineHandler(
    private val project: Project,
) {
    fun updateStatusLine(mode: NeovimMode) {
        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(NEOVIM_MODE_ID)
        check(widget is NeovimModeWidget) { "NeovimModeWidget not found in status bar" }
        widget.updateMode(mode)
    }
}
