local M = {}

function M.cursor(bufferId, line, col, curswant)
    vim.opt.eventignorewin = "all"
    vim.api.nvim_set_current_buf(bufferId)
    vim.fn.cursor({ line + 1, col + 1, 0, curswant + 1 })
    vim.schedule(function()
        vim.opt.eventignorewin = ""
    end)
end

return M
