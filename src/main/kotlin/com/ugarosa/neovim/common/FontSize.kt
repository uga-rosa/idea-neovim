package com.ugarosa.neovim.common

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FontSize(
    val width: Int,
    val height: Int,
) {
    companion object {
        suspend fun fromEditorEx(editor: EditorEx): FontSize =
            withContext(Dispatchers.EDT) {
                val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
                val metrics = editor.contentComponent.getFontMetrics(font)
                val width = metrics.charWidth('W')
                val height = metrics.height
                FontSize(width, height)
            }
    }
}
