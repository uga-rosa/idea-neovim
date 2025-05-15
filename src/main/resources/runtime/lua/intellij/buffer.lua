local M = {}

function M.activate(buffer_id)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(buffer_id)
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

function M.cursor(buffer_id, line, col, curswant)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(buffer_id)
	vim.fn.cursor({ line + 1, col + 1, 0, curswant + 1 })
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

return M
