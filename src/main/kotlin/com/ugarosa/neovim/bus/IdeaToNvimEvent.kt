package com.ugarosa.neovim.bus

import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition

sealed interface IdeaToNvimEvent

sealed interface IdeaDocumentChanged : IdeaToNvimEvent {
    val bufferId: BufferId
    val ignoreIncrement: Int

    data class NearCursor(
        override val bufferId: BufferId,
        val cursor: NvimPosition,
        val beforeDelete: Int,
        val afterDelete: Int,
        val text: String,
    ) : IdeaDocumentChanged {
        override val ignoreIncrement = beforeDelete + afterDelete + (if (text.isEmpty()) 0 else 1)
    }

    data class FarCursor(
        override val bufferId: BufferId,
        val start: NvimPosition,
        val end: NvimPosition,
        val replacement: List<String>,
    ) : IdeaDocumentChanged {
        override val ignoreIncrement = 1
    }
}

data class IdeaCaretMoved(
    val bufferId: BufferId,
    val pos: NvimPosition,
) : IdeaToNvimEvent

data class EditorSelected(
    val editor: EditorEx,
) : IdeaToNvimEvent

data class ChangeModifiable(
    val editor: EditorEx,
) : IdeaToNvimEvent
