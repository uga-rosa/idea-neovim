local M = {}

function M.cursor(bufferId, line, col, curswant)
	vim.opt.eventignorewin = "all"
	vim.api.nvim_set_current_buf(bufferId)
	vim.fn.cursor({ line + 1, col + 1, 0, curswant + 1 })
	vim.schedule(function()
		vim.opt.eventignorewin = ""
	end)
end

---@param bufferId number
---@param offset number 0-based char offset
---@return number line 0-based line number
---@return number col 0-based column number
local function offset_to_pos(bufferId, offset)
	local ff = vim.api.nvim_get_option_value("fileformat", { scope = "local" })
	local nl_len = ff == "dos" and 2 or 1

	local lines = vim.api.nvim_buf_get_lines(bufferId, 0, -1, false)
	local rem = offset
	for i, line in ipairs(lines) do
		local char_cnt = vim.fn.strchars(line)
		if rem <= char_cnt then
			local prefix = vim.fn.strcharpart(line, 0, rem)
			return i - 1, #prefix
		end
		rem = rem - (char_cnt + nl_len)
	end
	local last = #lines
	local tail = lines[last] or ""
	return last - 1, #tail
end

function M.set_text(bufferId, start, end_, lines)
	local start_line, start_col = offset_to_pos(bufferId, start)
	local end_line, end_col = offset_to_pos(bufferId, end_)
	vim.api.nvim_buf_set_text(bufferId, start_line, start_col, end_line, end_col, lines)
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
