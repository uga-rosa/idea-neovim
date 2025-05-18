package com.ugarosa.neovim.window

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.tabs.JBTabs
import com.ugarosa.neovim.common.GridSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.Container
import javax.swing.JPanel

sealed interface EditorLayout {
    data class HorizontalSplit(
        val top: EditorLayout,
        val bottom: EditorLayout,
    ) : EditorLayout

    data class VerticalSplit(
        val left: EditorLayout,
        val right: EditorLayout,
    ) : EditorLayout

    sealed interface Leaf : EditorLayout {
        data class Grid(
            val window: EditorWindow,
            val editor: EditorEx,
            val size: GridSize,
        ) : Leaf

        data class Diff(
            val window: EditorWindow,
            val left: Grid,
            val right: Grid,
        ) : Leaf

        data class Patch(
            val window: EditorWindow,
            val left: Grid,
            val mid: Grid,
            val right: Grid,
        ) : Leaf

        companion object {
            suspend fun fromWindow(window: EditorWindow): Leaf? {
                val composite = window.selectedComposite ?: return null
                val grids =
                    composite.allEditors
                        .flatMap { extractEditors(it) }
                        .map { Grid(window, it, GridSize.fromEditorEx(it)) }
                        .sortedBy { it.editor.component.x }
                return when (grids.size) {
                    0 -> null
                    1 -> grids[0]
                    2 -> Diff(window, grids[0], grids[1])
                    3 -> Patch(window, grids[0], grids[1], grids[2])
                    else -> error("Unexpected number of editors: ${grids.size}")
                }
            }

            private fun extractEditors(fileEditor: FileEditor): List<EditorEx> {
                val all =
                    if (fileEditor is TextEditor) {
                        listOf(fileEditor.editor)
                    } else {
                        collectEditorsFromComponent(fileEditor.component)
                    }
                return all.filterIsInstance<EditorEx>()
            }

            private fun collectEditorsFromComponent(c: Component): List<Editor> {
                return when (c) {
                    is EditorComponentImpl -> listOf(c.editor)
                    is Container -> c.components.flatMap { collectEditorsFromComponent(it) }
                    else -> emptyList()
                }
            }
        }
    }

    companion object Parser {
        suspend fun current(project: Project): EditorLayout? =
            withContext(Dispatchers.EDT) {
                val manager = FileEditorManagerEx.getInstanceEx(project)
                val splitters = manager.splitters
                val tabsToWindowMap = manager.windows.associateBy { it.tabbedPane.tabs }
                parseComponent(splitters, tabsToWindowMap)
            }

        private suspend fun parseComponent(
            component: Component,
            tabsToWindowMap: Map<JBTabs, EditorWindow>,
        ): EditorLayout? =
            when (component) {
                is Splitter -> {
                    val a = parseComponent(component.firstComponent, tabsToWindowMap)
                    val second = parseComponent(component.secondComponent, tabsToWindowMap)
                    when {
                        a == null && second == null -> null
                        a == null -> second
                        second == null -> a
                        component.orientation -> HorizontalSplit(a, second)
                        else -> VerticalSplit(a, second)
                    }
                }

                is JBTabs -> {
                    tabsToWindowMap[component]?.let { Leaf.fromWindow(it) }
                }

                is JPanel -> {
                    component.components
                        .mapNotNull { parseComponent(it, tabsToWindowMap) }
                        .singleOrNull()
                }

                else -> null
            }
    }
}
