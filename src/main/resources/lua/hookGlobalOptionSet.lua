local chanId = ...

local global_group = vim.api.nvim_create_augroup("IdeaNeovim:GlobalOptionSet", { clear = true })
vim.api.nvim_create_augroup("IdeaNeovim:LocalOptionSet", { clear = true })

vim.api.nvim_create_autocmd("OptionSet", {
	group = global_group,
	pattern = { "filetype", "selection", "scrolloff", "sidescrolloff" },
	callback = function(event)
		if vim.v.option_type ~= "global" then
			return
		end
		-- [bufferId, scope, name, value]
		vim.rpcnotify(chanId, "nvim_option_set", -1, "global", event.match, vim.v.option_new)
	end,
})
