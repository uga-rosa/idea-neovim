local M = {}

function M.activate(buffer_id)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(buffer_id)
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

function M.cursor(buffer_id, lnum, col, curswant)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(buffer_id)
	vim.fn.cursor({ lnum, col + 1, 0, curswant })
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

return M
