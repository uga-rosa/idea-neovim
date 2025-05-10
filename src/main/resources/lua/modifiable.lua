local bufferId = ...

vim.api.nvim_set_option_value("buftype", "", { buf = bufferId })
vim.api.nvim_set_option_value("modifiable", true, { buf = bufferId })
