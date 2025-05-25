package com.ugarosa.neovim.config

import com.ugarosa.neovim.config.nvim.option.Filetype
import com.ugarosa.neovim.config.nvim.option.Scrolloff
import com.ugarosa.neovim.config.nvim.option.Selection
import com.ugarosa.neovim.config.nvim.option.Sidescrolloff

data class NvimOption(
    val filetype: Filetype,
    val selection: Selection,
    val scrolloff: Scrolloff,
    val sidescrolloff: Sidescrolloff,
)
