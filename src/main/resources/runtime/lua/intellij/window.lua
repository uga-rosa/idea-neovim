local M = {}

function M.cursor(windowId, line, col, curswant)
    vim.opt.eventignorewin = "all"
    vim.api.nvim_set_current_win(windowId)
    vim.fn.cursor({ line + 1, col + 1, 0, curswant + 1 })
    vim.schedule(function()
        vim.opt.eventignorewin = ""
    end)
end

function M.reset()
    local last_win
    for i, win in ipairs(vim.api.nvim_list_wins()) do
        if i == 1 then
            last_win = win
        else
            vim.api.nvim_win_close(win, true)
        end
    end
    return last_win
end

return M