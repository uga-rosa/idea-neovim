local chanId = ...

local group = vim.api.nvim_create_augroup("IdeaNeovim:CursorMove", { clear = true })

vim.api.nvim_create_autocmd("CursorMoved", {
	group = group,
	callback = vim.schedule_wrap(function()
		local bufferId = vim.api.nvim_get_current_buf()
		local pos = vim.fn.getcurpos()
		local _, lnum, col, _, curswant = unpack(pos)
		-- [bufferId, line, column]
		vim.rpcnotify(chanId, "nvim_cursor_move_event", bufferId, lnum, col - 1, curswant)
	end),
})
