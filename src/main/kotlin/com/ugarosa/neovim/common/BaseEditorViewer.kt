package com.ugarosa.neovim.common

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.ugarosa.neovim.highlight.NeovimHighlightManager

private val CUSTOM_EDITOR_KEY = Key.create<Boolean>("CUSTOM_EDITOR_KEY")

fun isCustomEditor(editor: EditorEx): Boolean {
    return editor.document.getUserData(CUSTOM_EDITOR_KEY) == true
}

abstract class BaseEditorViewer(
    project: Project,
) {
    protected val document =
        EditorFactory.getInstance()
            .createDocument("").apply {
                putUserData(CUSTOM_EDITOR_KEY, true)
            }
    protected val editor: Editor =
        EditorFactory.getInstance().createViewer(
            document,
            project,
            EditorKind.PREVIEW,
        ).apply {
            settings.apply {
                // Simple viewer
                isLineNumbersShown = false
                isIndentGuidesShown = false
                isFoldingOutlineShown = false
                isRightMarginShown = false
                isCaretRowShown = false
                isLineMarkerAreaShown = false
                isAdditionalPageAtBottom = false
                isBlinkCaret = false
                // Use soft wraps
                isUseSoftWraps = true
            }
        }
    val component = editor.component

    protected val highlightManager = service<NeovimHighlightManager>()
}
