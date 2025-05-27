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
	"<Cmd>setl backspace=eol,start<CR>",
	"<Cmd>setl softtabstop=0<CR>",
	"<Cmd>setl nosmarttab<CR>",
	"<Cmd>setl textwidth=0<CR>",
}, ""))
local reset_options = vim.keycode(table.concat({
	"<Cmd>setl backspace=%s<CR>",
	"<Cmd>setl softtabstop=%s<CR>",
	"<Cmd>setl %ssmarttab<CR>",
	"<Cmd>setl textwidth=%s<CR>",
}, ""))

function M.send_repeatable_change(before, after, text)
	local keys = ""
	keys = keys .. bs:rep(before)
	keys = keys .. del:rep(after)

	if before > 0 then
		local backspace = vim.api.nvim_get_option_value("backspace", { scope = "local" })
		local softtabstop = vim.api.nvim_get_option_value("softtabstop", { scope = "local" })
		local smarttab = vim.api.nvim_get_option_value("smarttab", { scope = "local" }) and "" or "no"
		local textwidth = vim.api.nvim_get_option_value("textwidth", { scope = "local" })
		keys = set_options .. keys .. reset_options:format(backspace, softtabstop, smarttab, textwidth)
	end

	vim.opt.eventignorewin = "all"
	vim.api.nvim_feedkeys(keys, "n", false)
	vim.api.nvim_paste(text, false, -1)
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

return M
