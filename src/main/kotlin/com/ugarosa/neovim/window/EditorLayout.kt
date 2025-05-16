package com.ugarosa.neovim.window

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.ugarosa.neovim.common.GridSize
import java.awt.Component
import java.awt.Container

sealed interface EditorLayout {
    data class HorizontalSplit(
        val top: EditorLayout,
        val bottom: EditorLayout,
    ) : EditorLayout

    data class VerticalSplit(
        val left: EditorLayout,
        val right: EditorLayout,
    ) : EditorLayout

    sealed class Leaf(
        val window: EditorWindow,
    ) : EditorLayout {
        class Grid(
            window: EditorWindow,
            val editor: Editor,
            val size: GridSize,
        ) : Leaf(window) {
            override fun toString(): String {
                return "Single($editor, $size)"
            }
        }

        class Diff(
            window: EditorWindow,
            val grids: List<Grid>,
        ) : Leaf(window) {
            override fun toString(): String {
                return "Diff($grids)"
            }
        }

        companion object {
            suspend fun fromWindow(window: EditorWindow): Leaf? {
                val composite = window.selectedComposite ?: return null
                val grids =
                    composite.allEditors
                        .flatMap { extractEditors(it) }
                        .map { Grid(window, it, GridSize.fromEditorEx(it)) }
                        .sortedBy { it.editor.component.x }
                return if (grids.isEmpty()) {
                    null
                } else if (grids.size == 1) {
                    grids.first()
                } else {
                    Diff(window, grids)
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
}
