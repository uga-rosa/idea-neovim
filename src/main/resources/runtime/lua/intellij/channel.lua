local M = {}
M.chan_id = -1

function M.install(chan_id)
	M.chan_id = chan_id
end

function M.request(method, ...)
	return vim.rpcrequest(M.chan_id, method, ...)
end

function M.notify(method, ...)
	vim.rpcnotify(M.chan_id, method, ...)
end

return M
