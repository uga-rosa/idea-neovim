package com.ugarosa.neovim.config

import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Selection
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff

data class NeovimOption(
    val filetype: Filetype,
    val selection: Selection,
    val scrolloff: Scrolloff,
    val sidescrolloff: Sidescrolloff,
)
