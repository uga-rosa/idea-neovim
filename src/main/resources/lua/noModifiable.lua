local bufferId = ...

vim.api.nvim_set_option_value("buftype", "nofile", { buf = bufferId })
vim.api.nvim_set_option_value("modifiable", false, { buf = bufferId })
