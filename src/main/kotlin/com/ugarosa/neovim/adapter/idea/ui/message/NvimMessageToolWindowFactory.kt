package com.ugarosa.neovim.adapter.idea.ui.message

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

object NeovimMessageIcon {
    val base = IconLoader.getIcon("/icons/neovim-message@20x20.svg", javaClass)
    private val badge = IconLoader.getIcon("/icons/green-circle.svg", javaClass)
    val withBadge = OverlayIcon(base, badge)
}

class NeovimMessageToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        toolWindow.setIcon(NeovimMessageIcon.base)

        val contentFactory = ContentFactory.getInstance()

        val liveView = project.service<MessageLiveView>()
        val liveContent = contentFactory.createContent(liveView.component, MessageLiveView.TAB_TITLE, false)
        toolWindow.contentManager.addContent(liveContent)

        val historyView = project.service<MessageHistoryView>()
        val historyContent = contentFactory.createContent(historyView.component, MessageHistoryView.TAB_TITLE, false)
        toolWindow.contentManager.addContent(historyContent)

        toolWindow.contentManager.setSelectedContent(liveContent)
    }

    companion object {
        const val WINDOW_ID = "Neovim Messages"
    }
}
