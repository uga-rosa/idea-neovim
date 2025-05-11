local M = {}

function M.get_global()
	local opt = { scope = "global" }
	return {
		selection = vim.api.nvim_get_option_value("selection", opt),
		scrolloff = vim.api.nvim_get_option_value("scrolloff", opt),
		sidescrolloff = vim.api.nvim_get_option_value("sidescrolloff", opt),
	}
end

function M.get_local(buffer_id)
	local buf_opt = { scope = "local", buf = buffer_id }
	local win_id = vim.api.nvim_get_current_win()
	local win_opt = { scope = "local", win = win_id }
	return {
		filetype = vim.api.nvim_get_option_value("filetype", buf_opt),
		scrolloff = vim.api.nvim_get_option_value("scrolloff", win_opt),
		sidescrolloff = vim.api.nvim_get_option_value("sidescrolloff", win_opt),
	}
end

function M.set_filetype(buffer_id, path)
	local filetype = vim.filetype.match({ filename = path })
	vim.api.nvim_set_option_value("filetype", filetype, { scope = "local", buf = buffer_id })
end

function M.set_writable(buffer_id)
	vim.api.nvim_set_option_value("buftype", "", { scope = "local", buf = buffer_id })
	vim.api.nvim_set_option_value("modified", true, { scope = "local", buf = buffer_id })
end

function M.set_no_writable(buffer_id)
	vim.api.nvim_set_option_value("buftype", "nofile", { scope = "local", buf = buffer_id })
	vim.api.nvim_set_option_value("modified", false, { scope = "local", buf = buffer_id })
end

return M
