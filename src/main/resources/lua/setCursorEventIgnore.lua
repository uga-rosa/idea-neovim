local bufferId, lnum, col, curswant = ...

vim.opt.eventignorewin = "all"

vim.api.nvim_set_current_buf(bufferId)
vim.fn.cursor({ lnum, col, 0, curswant })

vim.schedule(function()
	vim.opt.eventignorewin = ""
end)
