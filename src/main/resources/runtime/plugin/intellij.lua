local win_id = vim.api.nvim_get_current_win()

-- Simplify grid conversion between Neovim and IntelliJ
vim.api.nvim_set_option_value("wrap", false, { win = win_id })
vim.api.nvim_set_option_value("number", false, { win = win_id })
vim.api.nvim_set_option_value("signcolumn", "no", { win = win_id })

-- Enforce single window
vim.api.nvim_create_autocmd("WinNew", {
	group = vim.api.nvim_create_augroup("IdeaNeovim:EnforceSingleWindow", { clear = true }),
	callback = vim.schedule_wrap(function()
		for _, win in ipairs(vim.api.nvim_list_wins()) do
			if win ~= win_id then
				vim.api.nvim_win_close(win, true)
			end
		end
	end),
})
