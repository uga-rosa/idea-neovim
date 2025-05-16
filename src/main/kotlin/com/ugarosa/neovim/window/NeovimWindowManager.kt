package com.ugarosa.neovim.window

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.tabs.JBTabs
import com.ugarosa.neovim.logger.myLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Component
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class NeovimWindowManager(
    private val project: Project,
) {
    private val logger = myLogger()

    suspend fun walkSplitLayout() =
        withContext(Dispatchers.EDT) {
            val manager = FileEditorManagerEx.getInstanceEx(project)
            val splitters = manager.splitters
            val tabsToWindowMap = manager.windows.associateBy { it.tabbedPane.tabs }
            val layout = extraLayout(splitters, tabsToWindowMap)
            logger.warn("Layout: $layout")
        }

    private suspend fun extraLayout(
        component: Component,
        tabsToWindowMap: Map<JBTabs, EditorWindow>,
    ): EditorLayout? {
        return when (component) {
            is Splitter -> {
                val first = extraLayout(component.firstComponent, tabsToWindowMap)
                val second = extraLayout(component.secondComponent, tabsToWindowMap)

                if (first == null && second == null) {
                    null
                } else if (first == null) {
                    second
                } else if (second == null) {
                    first
                } else if (component.orientation) {
                    EditorLayout.HorizontalSplit(first, second)
                } else {
                    EditorLayout.VerticalSplit(first, second)
                }
            }

            is JBTabs -> {
                val window = tabsToWindowMap[component] ?: return null
                EditorLayout.Leaf.fromWindow(window)
            }

            is JPanel -> {
                component.components
                    .mapNotNull { extraLayout(it, tabsToWindowMap) }
                    .singleOrNull()
            }

            else -> null
        }
    }
}
