package com.ugarosa.neovim.action

import com.intellij.openapi.editor.Editor

interface NeovimActionManager {
    suspend fun executeAction(
        actionId: String,
        editor: Editor?,
    )
}
