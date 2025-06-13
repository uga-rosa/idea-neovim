local win_id = vim.api.nvim_get_current_win()

-- Simplify grid conversion between Neovim and IntelliJ
vim.api.nvim_set_option_value("wrap", false, { win = win_id })
vim.api.nvim_set_option_value("number", false, { win = win_id })
vim.api.nvim_set_option_value("signcolumn", "no", { win = win_id })

require("intellij.hook")
