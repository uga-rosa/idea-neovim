package com.ugarosa.neovim.cmdline

import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent

interface NeovimCmdlinePopup {
    suspend fun handleEvent(event: CmdlineEvent)

    suspend fun destroy()
}
