local winId = vim.api.nvim_get_current_win()
vim.api.nvim_create_autocmd("WinNew", {
	group = vim.api.nvim_create_augroup("IdeaNeovim:EnforceSingleWindow", { clear = true }),
	callback = vim.schedule_wrap(function()
		for _, win in ipairs(vim.api.nvim_list_wins()) do
			if win ~= winId then
				vim.api.nvim_win_close(win, true)
			end
		end
	end),
})
