package com.ugarosa.neovim.bus

import com.intellij.openapi.editor.ex.EditorEx
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.position.NvimPosition

sealed interface IdeaToNvimEvent

data class IdeaDocumentChange(
    val bufferId: BufferId,
    val start: NvimPosition,
    val end: NvimPosition,
    val replacement: List<String>,
) : IdeaToNvimEvent

data class IdeaCaretMoved(
    val bufferId: BufferId,
    val pos: NvimPosition,
    val offset: Int,
) : IdeaToNvimEvent

data class EditorSelected(
    val editor: EditorEx,
) : IdeaToNvimEvent

data class ChangeModifiable(
    val editor: EditorEx,
) : IdeaToNvimEvent

data class EscapeInsert(
    val editor: EditorEx,
) : IdeaToNvimEvent
