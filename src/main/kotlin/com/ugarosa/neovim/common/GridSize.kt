package com.ugarosa.neovim.common

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.ex.EditorEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GridSize(
    val width: Int,
    val height: Int,
) {
    companion object {
        suspend fun fromEditorEx(editor: EditorEx): GridSize =
            withContext(Dispatchers.EDT) {
                val fontSize = FontSize.fromEditorEx(editor)
                val charWidth = fontSize.width
                val lineHeight = editor.lineHeight

                val (pixelW, pixelH) = editor.contentComponent.visibleRect.let { it.width to it.height }
                val width = (pixelW / charWidth).coerceAtLeast(1)
                val height = (pixelH / lineHeight).coerceAtLeast(1)

                GridSize(width, height)
            }
    }
}
