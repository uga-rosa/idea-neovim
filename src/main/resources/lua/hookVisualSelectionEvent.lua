local chanId = ...

vim.api.nvim_create_autocmd({ "CursorMoved", "ModeChanged" }, {
	group = vim.api.nvim_create_augroup("IdeaNeovim:VisualSelection", { clear = true }),
	callback = function()
		local mode = vim.api.nvim_get_mode().mode:sub(1, 1)
		if mode ~= "v" and mode ~= "V" and mode ~= "\22" then
			return
		end

		local bufferId = vim.api.nvim_get_current_buf()
		local vpos = vim.fn.getpos("v")
		local cpos = vim.fn.getpos(".")
		local regionPos = vim.fn.getregionpos(vpos, cpos, { type = mode })
		local regions = vim.iter(regionPos)
			:map(function(arg)
				local startPos, endPos = unpack(arg)
				return { startPos[2], startPos[3] - 1, endPos[3] - 1 }
			end)
			:totable()
		-- [bufferId, regions]
		-- regions = []{ row (1-index), startCol (0-index), endCol (0-index) }
		vim.rpcnotify(chanId, "nvim_visual_selection_event", bufferId, regions)
	end,
})
