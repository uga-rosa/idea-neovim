local chanId = ...

local group = vim.api.nvim_create_augroup("IdeaNeovim:ModeChange", { clear = true }),

vim.api.nvim_create_autocmd("ModeChanged", {
    group = group,
    callback = function()
        local bufferId = vim.api.nvim_get_current_buf()
        -- [bufferId, mode]
        vim.rpcnotify(chanId, "nvim_mode_change", bufferId, vim.v.event.new_mode)
    end,
})

vim.api.nvim_create_autocmd("BufEnter", {
    group = group,
    callback = function()
        local bufferId = vim.api.nvim_get_current_buf()
        local mode = vim.api.nvim_get_mode().mode
        -- [bufferId, mode]
        vim.rpcnotify(chanId, "nvim_mode_change", bufferId, mode)
    end,
})
