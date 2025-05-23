local M = {}

function M.cursor(bufferId, line, col, curswant)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(bufferId)
	vim.fn.cursor({ line + 1, col + 1, 0, curswant + 1 })
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

local bs = vim.keycode("<BS>")
local del = vim.keycode("<Del>")
local set_options = vim.keycode(table.concat({
	"<Cmd>setlocal backspace=eol,start<CR>",
	"<Cmd>setlocal softtabstop=0<CR>",
	"<Cmd>setlocal nosmarttab<CR>",
	"<Cmd>setlocal textwidth=0<CR>",
}, ""))
local reset_options = vim.keycode(table.concat({
	"<Cmd>setlocal backspace=%s<CR>",
	"<Cmd>setlocal softtabstop=%s<CR>",
	"<Cmd>setlocal %ssmarttab<CR>",
	"<Cmd>setlocal textwidth=%s<CR>",
}, ""))

function M.send_deletion(before, after)
	local text = ""
	text = text .. bs:rep(before)
	text = text .. del:rep(after)

	if before > 0 then
		local backspace = vim.api.nvim_get_option_value("backspace", { scope = "local" })
		local softtabstop = vim.api.nvim_get_option_value("softtabstop", { scope = "local" })
		local smarttab = vim.api.nvim_get_option_value("smarttab", { scope = "local" }) and "" or "no"
		local textwidth = vim.api.nvim_get_option_value("textwidth", { scope = "local" })
		text = set_options .. text .. reset_options:format(backspace, softtabstop, smarttab, textwidth)
	end

	vim.api.nvim_feedkeys(text, "n", false)
end

return M
