local chanId = ...

vim.api.nvim_create_autocmd({ "CursorMoved", "ModeChanged" }, {
    group = vim.api.nvim_create_augroup("IdeaNeovim:VisualSelection", { clear = true }),
    callback = function()
        local mode = vim.api.nvim_get_mode().mode:sub(1, 1)
        if mode ~= "v" and mode ~= "V" and mode ~= "\22" then
            return
        end

        local bufferId = vim.api.nvim_get_current_buf()
        local vpos = vim.fn.getpos("v")
        local cpos = vim.fn.getpos(".")
        -- (row, col) should follow (1, 0) index in neovim
        local start_row, start_col = vpos[2], vpos[3] - 1
        local end_row, end_col = cpos[2], cpos[3] - 1
        if start_row > end_row or (start_row == end_row and start_col > end_col) then
            start_row, start_col, end_row, end_col = end_row, end_col, start_row, start_col
        end
        -- [bufferId, mode, start_row, start_col, end_row, end_col]
        vim.rpcnotify(chanId, "nvim_visual_selection_event", bufferId, mode, start_row, start_col, end_row, end_col)
    end,
})
