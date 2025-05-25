package com.ugarosa.neovim.adapter.idea.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.ugarosa.neovim.domain.highlight.NvimHighlightManager
import java.awt.Graphics
import java.awt.Rectangle

private val CUSTOM_EDITOR_KEY = Key.create<Boolean>("CUSTOM_EDITOR_KEY")

fun isCustomEditor(editor: EditorEx): Boolean {
    return editor.document.getUserData(CUSTOM_EDITOR_KEY) == true
}

abstract class BaseEditorViewer(
    project: Project,
) {
    protected val highlightManager = service<NvimHighlightManager>()

    protected val document =
        EditorFactory.getInstance()
            .createDocument("").apply {
                putUserData(CUSTOM_EDITOR_KEY, true)
            }
    protected val editor: EditorEx =
        EditorFactory.getInstance().createEditor(document, project)
            .let { it as EditorEx }
            .apply {
                settings.apply {
                    // Simple viewer
                    isLineNumbersShown = false
                    isIndentGuidesShown = false
                    isFoldingOutlineShown = false
                    isRightMarginShown = false
                    isCaretRowShown = false
                    isLineMarkerAreaShown = false
                    isAdditionalPageAtBottom = false
                    // Use soft wraps
                    isUseSoftWraps = true
                }
                backgroundColor = highlightManager.defaultBackground
            }
    val component = editor.component

    fun getHeight(): Int = document.lineCount * editor.lineHeight

    protected fun drawFakeCaret() {
        val inlayModel = editor.inlayModel

        inlayModel.getInlineElementsInRange(0, document.textLength)
            .filter { it.renderer is CaretRenderer }
            .forEach { it.dispose() }

        val offset = editor.caretModel.offset

        inlayModel.addInlineElement(
            offset,
            true,
            CaretRenderer(),
        )
    }

    private class CaretRenderer : EditorCustomElementRenderer {
        override fun calcWidthInPixels(inlay: Inlay<*>): Int = 1

        override fun paint(
            inlay: Inlay<*>,
            g: Graphics,
            targetRect: Rectangle,
            textAttributes: TextAttributes,
        ) {
            val schema = EditorColorsManager.getInstance().globalScheme
            val color = schema.getColor(EditorColors.CARET_COLOR) ?: JBColor.WHITE
            g.color = color
            g.fillRect(targetRect.x, targetRect.y, 1, targetRect.height)
        }
    }
}
