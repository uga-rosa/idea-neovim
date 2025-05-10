local lnum, col, curswant = ...

local cur = vim.fn.getcurpos()
local _, cur_lnum, cur_col, _, cur_curswant = unpack(cur)

if lnum == cur_lnum and col == cur_col and curswant == cur_curswant then
	-- No need to move the cursor
	return
end

local mode = vim.api.nvim_get_mode().mode
if not (vim.startswith(mode, "i") or vim.startswith(mode, "c")) then
	vim.g.__idea_neovim_cursor_move_ignore_count = vim.g.__idea_neovim_cursor_move_ignore_count + 1
end

vim.fn.cursor({ lnum, col, 0, curswant })
