package com.ugarosa.neovim.message

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class NeovimMessageManager {
    private val logger = myLogger()

    init {
        // Reset icon when tool window is shown
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun toolWindowShown(toolWindow: ToolWindow) {
                    if (toolWindow.id != NeovimMessageToolWindowFactory.WINDOW_ID) return
                    invokeLater {
                        toolWindow.setIcon(NeovimMessageIcon.base)
                    }
                }
            },
        )
    }

    suspend fun handleMessageEvent(event: MessageEvent) =
        withContext(Dispatchers.EDT) {
            val project = focusProject() ?: return@withContext
            val livePane = project.service<MessageLivePane>()
            val historyPane = project.service<MessageHistoryPane>()

            logger.trace("Handling message event: $event")

            when (event) {
                is MessageEvent.Show -> {
                    livePane.updateModel(show = event)
                    historyPane.updateHistory(event)
                }

                is MessageEvent.ShowHistory -> {
                    livePane.updateModel(history = event)
                }

                is MessageEvent.Clear,
                is MessageEvent.ClearHistory,
                -> {
                    livePane.clear()
                }

                is MessageEvent.Flush -> {
                    toolWindow(project)?.let { tw ->
                        if (livePane.flush()) {
                            tw.setIcon(NeovimMessageIcon.withBadge)
                        } else {
                            tw.hide()
                        }
                    }
                }
            }
        }

    private fun toolWindow(project: Project): ToolWindow? =
        ToolWindowManager.getInstance(project)
            .getToolWindow(NeovimMessageToolWindowFactory.WINDOW_ID)
}
