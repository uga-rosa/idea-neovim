package com.ugarosa.neovim.cmdline

import com.intellij.openapi.editor.Editor
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent

interface NeovimCmdlinePopup {
    fun attachTo(editor: Editor)

    suspend fun handleEvent(event: CmdlineEvent)
}
