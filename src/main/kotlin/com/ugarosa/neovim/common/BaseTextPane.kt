package com.ugarosa.neovim.common

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.ugarosa.neovim.highlight.NeovimHighlightManager
import com.ugarosa.neovim.logger.myLogger
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.text.StyledEditorKit

abstract class BaseTextPane : JTextPane() {
    protected val logger = myLogger()
    protected val highlightManager = service<NeovimHighlightManager>()

    init {
        font =
            runReadAction {
                val scheme = EditorColorsManager.getInstance().globalScheme
                Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
            }

        isEditable = false
        isFocusable = false
        editorKit = StyledEditorKit()
        isOpaque = true
        background = JBColor.background()
    }
}
