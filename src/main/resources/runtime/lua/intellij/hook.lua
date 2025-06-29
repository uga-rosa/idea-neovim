local channel = require("intellij.channel")

-- CursorMoved events
do
	local group = vim.api.nvim_create_augroup("IdeaNeovim:CursorMoved", { clear = true })

	local on_cursor_moved = function()
		local bufferId = vim.api.nvim_get_current_buf()
		local pos = vim.fn.getcurpos()
		local _, lnum, col, _, curswant = unpack(pos)
		-- [bufferId, line, column]
		channel.notify("IdeaNeovim:CursorMoved", bufferId, lnum - 1, col - 1, curswant - 1)
	end

	vim.api.nvim_create_autocmd("CursorMoved", {
		group = group,
		callback = on_cursor_moved,
	})
	vim.api.nvim_create_autocmd("ModeChanged", {
		group = group,
		pattern = "*:i",
		callback = on_cursor_moved,
	})
end

-- OptionSet events
do
	vim.api.nvim_create_autocmd("OptionSet", {
		group = vim.api.nvim_create_augroup("IdeaNeovim:OptionSet:Global", { clear = true }),
		pattern = { "filetype", "selection", "scrolloff", "sidescrolloff" },
		callback = function(event)
			if vim.v.option_type ~= "global" then
				return
			end
			-- [bufferId, scope, name, value]
			channel.notify("IdeaNeovim:OptionSet", -1, "global", event.match, vim.v.option_new)
		end,
	})

	local local_group = vim.api.nvim_create_augroup("IdeaNeovim:OptionSet:Local", { clear = true })
	vim.api.nvim_create_autocmd("BufNew", {
		group = local_group,
		callback = function(event)
			local buffer_id = event.buf
			vim.api.nvim_create_autocmd("OptionSet", {
				group = local_group,
				buffer = buffer_id,
				callback = function(event2)
					if vim.v.option_type ~= "local" then
						return
					end
					if
						not vim.tbl_contains({ "filetype", "selection", "scrolloff", "sidescrolloff" }, event2.match)
					then
						return
					end
					-- [bufferId, scope, name, value]
					channel.notify("IdeaNeovim:OptionSet", buffer_id, "local", event2.match, vim.v.option_new)
				end,
			})
		end,
	})
end

-- VisualSelection events
do
	vim.api.nvim_create_autocmd({ "CursorMoved", "ModeChanged" }, {
		group = vim.api.nvim_create_augroup("IdeaNeovim:VisualSelection", { clear = true }),
		callback = function()
			local mode = vim.api.nvim_get_mode().mode:sub(1, 1)
			if mode:find("[vV\22]") == nil then
				return
			end

			local buffer_id = vim.api.nvim_get_current_buf()
			local vpos = vim.fn.getpos("v")
			local cpos = vim.fn.getpos(".")
			local region_pos = vim.fn.getregionpos(vpos, cpos, { type = mode })
			local regions = vim.iter(region_pos)
				:map(function(arg)
					local startPos, endPos = unpack(arg)
					-- (0, 0) index, end-exclusive
					return { startPos[2] - 1, startPos[3] - 1, endPos[3] }
				end)
				:totable()
			-- [bufferId, regions]
			-- regions = []{ row, start_col, end_col }
			channel.notify("IdeaNeovim:VisualSelection", buffer_id, regions)
		end,
	})
end

-- ModeChanged events
do
	vim.api.nvim_create_autocmd("ModeChanged", {
		group = vim.api.nvim_create_augroup("IdeaNeovim:ModeChanged", { clear = true }),
		callback = function()
			local buffer_id = vim.api.nvim_get_current_buf()
			channel.notify("IdeaNeovim:ModeChanged", buffer_id, vim.v.event.new_mode)
		end,
	})
end

-- ExecIdeaAction command
do
	vim.api.nvim_create_user_command("ExecIdeaAction", function(opt)
		local action_id = opt.args
		channel.notify("IdeaNeovim:ExecIdeaAction", action_id)
	end, {
		nargs = 1,
	})
end
