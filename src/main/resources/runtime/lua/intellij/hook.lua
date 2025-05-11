local M = {}

function M.cursor_moved(chan_id)
	local group = vim.api.nvim_create_augroup("IdeaNeovim:CursorMoved", { clear = true })
	vim.api.nvim_create_autocmd("CursorMoved", {
		group = group,
		callback = vim.schedule_wrap(function()
			local bufferId = vim.api.nvim_get_current_buf()
			local pos = vim.fn.getcurpos()
			local _, lnum, col, _, curswant = unpack(pos)
			-- [bufferId, line, column]
			vim.rpcnotify(chan_id, "nvim_cursor_move_event", bufferId, lnum, col - 1, curswant)
		end),
	})
end

function M.global_option_set(chan_id)
	local group = vim.api.nvim_create_augroup("IdeaNeovim:OptionSet:Global", { clear = true })
	vim.api.nvim_create_autocmd("OptionSet", {
		group = group,
		pattern = { "filetype", "selection", "scrolloff", "sidescrolloff" },
		callback = function(event)
			if vim.v.option_type ~= "global" then
				return
			end
			-- [bufferId, scope, name, value]
			vim.rpcnotify(chan_id, "nvim_option_set_event", -1, "global", event.match, vim.v.option_new)
		end,
	})
end

local local_group = vim.api.nvim_create_augroup("IdeaNeovim:OptionSet:Local", { clear = true })
function M.local_option_set(chan_id, buffer_id)
	vim.api.nvim_create_autocmd("OptionSet", {
		group = local_group,
		buffer = buffer_id,
		callback = function(event)
			if vim.v.option_type ~= "local" then
				return
			end
			if not vim.tbl_contains({ "filetype", "selection", "scrolloff", "sidescrolloff" }, event.match) then
				return
			end
			-- [bufferId, scope, name, value]
			vim.rpcnotify(chan_id, "nvim_option_set_event", buffer_id, "local", event.match, vim.v.option_new)
		end,
	})
end

function M.visual_selection(chan_id)
	vim.api.nvim_create_autocmd({ "CursorMoved", "ModeChanged" }, {
		group = vim.api.nvim_create_augroup("IdeaNeovim:VisualSelection", { clear = true }),
		callback = function()
			local mode = vim.api.nvim_get_mode().mode:sub(1, 1)
			if mode ~= "v" and mode ~= "V" and mode ~= "\22" then
				return
			end

			local buffer_id = vim.api.nvim_get_current_buf()
			local vpos = vim.fn.getpos("v")
			local cpos = vim.fn.getpos(".")
			local region_pos = vim.fn.getregionpos(vpos, cpos, { type = mode })
			local regions = vim.iter(region_pos)
				:map(function(arg)
					local startPos, endPos = unpack(arg)
					return { startPos[2], startPos[3] - 1, endPos[3] - 1 }
				end)
				:totable()
			-- [bufferId, regions]
			-- regions = []{ row (1-index), startCol (0-index), endCol (0-index) }
			vim.rpcnotify(chan_id, "nvim_visual_selection_event", buffer_id, regions)
		end,
	})
end

return M
