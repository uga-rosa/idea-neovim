package com.ugarosa.neovim.buffer.document

import com.ugarosa.neovim.rpc.type.NeovimPosition

sealed interface DocChange {
    data class Input(
        val text: String,
    ) : DocChange

    data class SetText(
        val start: NeovimPosition,
        val end: NeovimPosition,
        val replacement: List<String>,
    ) : DocChange
}
