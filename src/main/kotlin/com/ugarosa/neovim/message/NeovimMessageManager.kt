package com.ugarosa.neovim.message

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.ugarosa.neovim.common.focusProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class NeovimMessageManager {
    suspend fun handleMessageEvent(event: MessageEvent) =
        withContext(Dispatchers.EDT) {
            val project = focusProject() ?: return@withContext
            val livePane = project.service<MessageLivePane>()
            val historyPane = project.service<MessageHistoryPane>()

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
                        if (livePane.isHide()) {
                            tw.hide()
                        } else {
                            livePane.flush()
                            tw.show {
                                val live = tw.contentManager.findContent(MessageLivePane.DISPLAY_NAME)
                                tw.contentManager.setSelectedContent(live)
                            }
                        }
                    }
                }
            }
        }

    private fun toolWindow(project: Project): ToolWindow? =
        ToolWindowManager.getInstance(project)
            .getToolWindow(NeovimMessageToolWindowFactory.WINDOW_ID)
}
