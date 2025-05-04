local chatId, bufferId = ...

vim.api.nvim_create_autocmd("OptionSet", {
    group = vim.api.nvim_create_augroup("IdeaNeovim:LocalOptionSet", { clear = false }),
    buffer = bufferId,
    callback = function(event)
        if (vim.v.option_type ~= "local") then
            return
        end
        local payload = {
            name = event.match,
            scope = "local",
            value = vim.v.option_new,
            buffer = bufferId,
        }
        vim.rpcnotify(chatId, "nvim_option_set", payload)
    end,
})