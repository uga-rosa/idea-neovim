local M = {}

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