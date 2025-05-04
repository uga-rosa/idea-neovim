local chatId, bufferId = ...
local pattern = { "filetype", "selection", "scrolloff", "sidescrolloff" },

vim.api.nvim_create_autocmd("OptionSet", {
    group = vim.api.nvim_create_augroup("IdeaNeovim:LocalOptionSet", { clear = false }),
    buffer = bufferId,
    callback = function(event)
        if (vim.v.option_type ~= "local") then
            return
        end
        if not vim.tbl_contains(pattern, event.match) then
            return
        end
        -- [bufferId, scope, name, value]
        vim.rpcnotify(chatId, "nvim_option_set", bufferId, "local", event.match, vim.v.option_new)
    end,
})