local opt = { scope = "global" }

return {
    selection = vim.api.nvim_get_option_value("selection", opt),
    scrolloff = vim.api.nvim_get_option_value("scrolloff", opt),
    sidescrolloff = vim.api.nvim_get_option_value("sidescrolloff", opt),
}