package com.ugarosa.neovim.adapter.idea.editor

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.config.nvim.NvimOptionManager
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.mode.NvimMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CaretShapeSyncAdapter(
    private val editor: EditorEx,
) {
    private val optionsManager = service<NvimOptionManager>()

    suspend fun apply(
        bufferId: BufferId,
        mode: NvimMode,
    ) {
        val option = optionsManager.getLocal(bufferId)
        withContext(Dispatchers.EDT) {
            if (mode.isCommand()) {
                changeCaretVisible(false)
            } else {
                changeCaretVisible(true)
            }
            editor.settings.isBlockCursor = mode.isBlock(option.selection)
        }
    }

    private fun changeCaretVisible(isVisible: Boolean) {
        editor.setCaretVisible(isVisible)
        editor.setCaretEnabled(isVisible)
        editor.contentComponent.repaint()
    }
}
