local bufferId, path = ...

vim.api.nvim_buf_set_name(bufferId, path)
local filetype = vim.filetype.match({ filename = path })
vim.api.nvim_set_option_value("filetype", filetype, { buf = bufferId })
