local bufferId = ...
local bufOpt = { scope = "local", buf = bufferId }
local winId = vim.api.nvim_get_current_win()
local winOpt = { scope = "local", win = winId }

return {
    filetype = vim.api.nvim_get_option_value("filetype", bufOpt),
    scrolloff = vim.api.nvim_get_option_value("scrolloff", winOpt),
    sidescrolloff = vim.api.nvim_get_option_value("sidescrolloff", winOpt),
}