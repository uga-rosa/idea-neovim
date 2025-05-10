local chanId = ...

vim.api.nvim_create_autocmd("ModeChanged", {
	group = vim.api.nvim_create_augroup("IdeaNeovim:ModeChange", { clear = true }),
	callback = function()
		local bufferId = vim.api.nvim_get_current_buf()
		-- [bufferId, mode]
		vim.rpcnotify(chanId, "nvim_mode_change_event", bufferId, vim.v.event.new_mode)
		-- If the new mode is insert mode, we need to send the cursor position
		-- since CursorMoved event will not be triggered.
		if vim.startswith(vim.v.event.new_mode, "i") then
			local pos = vim.fn.getcurpos()
			local _, lnum, col, _, curswant = unpack(pos)
			vim.rpcnotify(chanId, "nvim_cursor_move_event", bufferId, lnum, col - 1, curswant)
		end
	end,
})
