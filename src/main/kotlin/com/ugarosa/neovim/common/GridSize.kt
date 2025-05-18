package com.ugarosa.neovim.common

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.ex.EditorEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GridSize(
    val outerWidth: Int,
    val outerHeight: Int,
    val innerWidth: Int,
    val innerHeight: Int,
) {
    companion object {
        suspend fun fromEditorEx(editor: EditorEx): GridSize =
            withContext(Dispatchers.EDT) {
                val fontSize = FontSize.fromEditorEx(editor)
                val charWidth = fontSize.width
                val lineHeight = editor.lineHeight

                val (outerPixelW, outerPixelH) = editor.component.let { it.width to it.height }
                val outerWidth = (outerPixelW / charWidth).coerceAtLeast(1)
                val outerHeight = (outerPixelH / lineHeight).coerceAtLeast(1)

                val (innerPixelW, innerPixelH) = editor.contentComponent.visibleRect.let { it.width to it.height }
                val innerWidth = (innerPixelW / charWidth).coerceAtLeast(1)
                val innerHeight = (innerPixelH / lineHeight).coerceAtLeast(1)

                GridSize(outerWidth, outerHeight, innerWidth, innerHeight)
            }
    }
}
