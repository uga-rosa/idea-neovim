local M = {}

local f = vim.fn

local bs = vim.keycode("<BS>")
local del = vim.keycode("<Del>")
local left = vim.keycode("<C-g>U<Left>")

---AAA + BBB -> AAABBB<Left><Left><Left>
---@param input string
---@param input_after string
---@return string
local function input_oneline(input, input_after)
	return input .. input_after .. left:rep(f.strchars(input_after))
end

local function to_feedable(text)
	return text:gsub("<", "<lt>")
end

local set_options = vim.keycode(table.concat({
	"<Cmd>setl backspace=eol,start<CR>",
	"<Cmd>setl softtabstop=0<CR>",
	"<Cmd>setl nosmarttab<CR>",
	"<Cmd>setl textwidth=0<CR>",
	"<Cmd>setl noautoindent<CR>",
	"<Cmd>setl nosmartindent<CR>",
	"<Cmd>setl nocindent<CR>",
	"<Cmd>setl formatoptions=<CR>",
	"<Cmd>setl indentexpr=<CR>",
	"<Cmd>setl indentkeys=<CR>",
}, ""))
local reset_options = vim.keycode(table.concat({
	"<Cmd>setl backspace=%s<CR>",
	"<Cmd>setl softtabstop=%s<CR>",
	"<Cmd>setl %ssmarttab<CR>",
	"<Cmd>setl textwidth=%s<CR>",
	"<Cmd>setl %sautoindent<CR>",
	"<Cmd>setl %ssmartindent<CR>",
	"<Cmd>setl %scindent<CR>",
	"<Cmd>setl formatoptions=%s<CR>",
	"<Cmd>setl indentexpr=%s<CR>",
	"<Cmd>setl indentkeys=%s<CR>",
}, ""))

---@type {input: string, first_line: string, after_lines: string[]}[]
local holder = {}

---@param before number Delete chars before the cursor
---@param after number Delete chars after the cursor
---@param input string Text to insert at the cursor
---@param input_after string Text to insert after the cursor
---@return number ignore_increment
function M.input(before, after, input, input_after)
	local text = ""
	text = text .. bs:rep(before)
	text = text .. del:rep(after)

	local ignore_increment = before + after

	local split_input_after = vim.split(input_after, "\n")
	local first_line = table.remove(split_input_after, 1)
	local after_lines = split_input_after

	ignore_increment = ignore_increment + f.strchars(input .. first_line)
	input = input_oneline(input, first_line)
	input = to_feedable(input)
	text = text .. input

	if #after_lines > 0 then
		local key = #holder + 1
		holder[key] = after_lines
		text = text .. vim.keycode(([[<Cmd>lua require("intellij.insert")._input_multi_lines(%d)<CR>]]):format(key))
		ignore_increment = ignore_increment + 2
	end

	if before > 0 or input ~= "" then
		local backspace = vim.api.nvim_get_option_value("backspace", { scope = "local" })
		local softtabstop = vim.api.nvim_get_option_value("softtabstop", { scope = "local" })
		local smarttab = vim.api.nvim_get_option_value("smarttab", { scope = "local" }) and "" or "no"
		local textwidth = vim.api.nvim_get_option_value("textwidth", { scope = "local" })
		local autoindent = vim.api.nvim_get_option_value("autoindent", { scope = "local" }) and "" or "no"
		local smartindent = vim.api.nvim_get_option_value("smartindent", { scope = "local" }) and "" or "no"
		local cindent = vim.api.nvim_get_option_value("cindent", { scope = "local" }) and "" or "no"
		local formatoptions = vim.api.nvim_get_option_value("formatoptions", { scope = "local" })
		local indentexpr = vim.api.nvim_get_option_value("indentexpr", { scope = "local" })
		local indentkeys = vim.api.nvim_get_option_value("indentkeys", { scope = "local" })
		text = set_options
			.. text
			.. reset_options:format(
				backspace,
				softtabstop,
				smarttab,
				textwidth,
				autoindent,
				smartindent,
				cindent,
				formatoptions,
				indentexpr,
				indentkeys
			)
	end

	vim.api.nvim_feedkeys(text, "n", false)
	return ignore_increment
end

---@param key number
function M._input_multi_lines(key)
	local after_lines = table.remove(holder, key)

	local _, lnum, col, _, _ = unpack(f.getcurpos())
	local cur_line = f.getline(".")
	local pre_cursor = cur_line:sub(1, col - 1)
	local post_cursor = cur_line:sub(col)
	f.setline(lnum, pre_cursor)

	after_lines[#after_lines] = after_lines[#after_lines] .. post_cursor
	f.append(lnum, after_lines)
end

return M
