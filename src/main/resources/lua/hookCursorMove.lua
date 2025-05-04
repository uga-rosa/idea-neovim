local chanId = ...
vim.g.__idea_neovim_cursor_move_ignore_count = 0

vim.api.nvim_create_autocmd("CursorMoved", {
    group = vim.api.nvim_create_augroup("IdeaNeovim:CursorMove", { clear = true }),
    callback = function()
        if vim.g.__idea_neovim_cursor_move_ignore_count > 0 then
            vim.g.__idea_neovim_cursor_move_ignore_count = vim.g.__idea_neovim_cursor_move_ignore_count - 1
            return
        end
        local bufferId = vim.api.nvim_get_current_buf()
        local cursor = vim.api.nvim_win_get_cursor(0)
        -- [bufferId, line, column]
        vim.rpcnotify(chanId, "nvim_cursor_move", bufferId, cursor[1], cursor[2])
    end,
})