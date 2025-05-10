local bufferId = ...

vim.opt.eventignorewin = "all"

vim.api.nvim_set_current_buf(bufferId)

vim.schedule(function()
	vim.opt.eventignorewin = ""
end)
