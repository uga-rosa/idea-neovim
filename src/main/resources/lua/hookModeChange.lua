local chanId = ...

vim.api.nvim_create_autocmd("ModeChanged", {
	group = vim.api.nvim_create_augroup("IdeaNeovim:ModeChange", { clear = true }),
	callback = function()
		local bufferId = vim.api.nvim_get_current_buf()
		-- [bufferId, mode]
		vim.rpcnotify(chanId, "nvim_mode_change_event", bufferId, vim.v.event.new_mode)
	end,
})
