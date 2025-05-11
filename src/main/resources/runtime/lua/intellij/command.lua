local M = {}

function M.create_exec_action(chan_id)
	vim.api.nvim_create_user_command("ExecIdeaAction", function(opt)
		local buffer_id = vim.api.nvim_get_current_buf()
		local action_id = opt.args
		vim.rpcnotify(chan_id, "nvim_exec_idea_action_event", buffer_id, action_id)
	end, {
		nargs = 1,
	})
end

return M
