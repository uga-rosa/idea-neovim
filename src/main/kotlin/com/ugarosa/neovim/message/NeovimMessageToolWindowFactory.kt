package com.ugarosa.neovim.message

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory

class NeovimMessageToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val contentFactory = ContentFactory.getInstance()

        val livePane = project.service<MessageLivePane>()
        val liveScroll =
            JBScrollPane(livePane).apply {
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            }
        val liveContent = contentFactory.createContent(liveScroll, MessageLivePane.DISPLAY_NAME, false)
        toolWindow.contentManager.addContent(liveContent)

        val historyPane = project.service<MessageHistoryPane>()
        val historyScroll =
            JBScrollPane(historyPane).apply {
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            }
        val historyContent = contentFactory.createContent(historyScroll, MessageHistoryPane.DISPLAY_NAME, false)
        toolWindow.contentManager.addContent(historyContent)

        toolWindow.contentManager.setSelectedContent(liveContent)
    }

    companion object {
        const val WINDOW_ID = "Neovim Messages"
    }
}
