package com.ugarosa.neovim.config.neovim

import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Selection
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.rpc.BufferId

data class NeovimOption(
    val filetype: Filetype,
    val selection: Selection,
    val scrolloff: Scrolloff,
    val sidescrolloff: Sidescrolloff,
)

interface NeovimOptionManager {
    suspend fun initializeGlobal()

    suspend fun initializeLocal(bufferId: BufferId)

    suspend fun getGlobal(): NeovimGlobalOptions

    suspend fun getLocal(bufferId: BufferId): NeovimOption

    suspend fun putGlobal(
        key: String,
        value: Any,
    )

    suspend fun putLocal(
        bufferId: BufferId,
        key: String,
        value: Any,
    )
}
