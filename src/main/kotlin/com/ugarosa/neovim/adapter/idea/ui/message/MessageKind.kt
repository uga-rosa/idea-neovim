package com.ugarosa.neovim.adapter.idea.ui.message

enum class MessageKind(val value: String) {
    BufWrite("bufwrite"),
    Confirm("confirm"),
    ErrMsg("emsg"),
    Echo("echo"),
    EchoMsg("echomsg"),
    EchoErr("echoerr"),
    Completion("completion"),
    ListCmd("list_cmd"),
    LuaErr("lua_error"),
    LuaPrint("lua_print"),
    RpcErr("rpc_error"),
    ReturnPrompt("return_prompt"),
    QuickFix("quickfix"),
    SearchCmd("search_cmd"),
    SearchCount("search_count"),
    ShellErr("shell_err"),
    ShellOut("shell_out"),
    ShellRet("shell_ret"),
    Undo("undo"),
    Verbose("verbose"),
    WildList("wildlist"),
    WarnMsg("wmsg"),
    Unknown(""),

    // For "msg_history_show"
    History("history"),
    ;

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromValue(value: String): MessageKind {
            return map[value] ?: Unknown
        }
    }
}
