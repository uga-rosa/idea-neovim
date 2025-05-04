vim.api.nvim_create_autocmd("WinNew", {
    group = vim.api.nvim_create_augroup("IdeaNeovim:EnforceSingleWindow", { clear = true }),
    callback = vim.schedule_wrap(function()
        local current = vim.api.nvim_get_current_win()
        if #vim.api.nvim_list_wins() > 1 then
            vim.api.nvim_win_close(current, true)
        end
        vim.notify("Neovim is restricted to a single window under IDEA integration.", vim.log.levels.WARN)
    end)
})