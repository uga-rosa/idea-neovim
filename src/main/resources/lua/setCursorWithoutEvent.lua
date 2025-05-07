local target_row, target_col = ...

local cur = vim.api.nvim_win_get_cursor(0)
local cur_row, cur_col = cur[1], cur[2]

if target_row == cur_row and target_col == cur_col then
    -- No need to move the cursor
    return
end

local mode = vim.api.nvim_get_mode().mode
if not (vim.startswith(mode, "i") or vim.startswith(mode, "c")) then
    vim.g.__idea_neovim_cursor_move_ignore_count = vim.g.__idea_neovim_cursor_move_ignore_count + 1
end

vim.api.nvim_win_set_cursor(0, { ... })
