local chanId = ...

vim.api.nvim_create_user_command("ExecIdeaAction", function(opt)
	local bufferId = vim.api.nvim_get_current_buf()
	local actionId = opt.args
	vim.rpcnotify(chanId, "nvim_exec_idea_action_event", bufferId, actionId)
end, {
	nargs = 1,
})
