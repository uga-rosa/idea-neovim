package com.ugarosa.neovim.adapter.idea.ui.message

/**
 * **ui-messages**
 *
 * Activated by the `ext_messages` `ui-option`.
 * Activates `ui-linegrid` and `ui-cmdline` implicitly.
 *
 * This UI extension delegates presentation of messages and dialogs. Messages
 * that would otherwise render in the message/cmdline screen space, are emitted
 * as UI events.
 *
 * Nvim will not allocate screen space for the cmdline or messages. 'cmdheight'
 * will be set to zero, but can be changed and used for the replacing cmdline or
 * message window. Cmdline state is emitted as `ui-cmdline` events, which the UI
 * must handle.
 */
sealed interface MessageEvent {
    /**
     * Display a message to the user.
     *
     * `["msg_show", kind, content, replace_last, history]`
     */
    data class Show(
        val kind: MessageKind,
        val content: List<MsgChunk>,
        val replaceLast: Boolean,
        val history: Boolean,
    ) : MessageEvent

    /**
     * Clear all messages currently displayed by "msg_show". (Messages sent
     * by other "msg_" events below will not be affected).
     *
     * `["msg_clear"]`
     */
    data object Clear : MessageEvent

    // Ignore "msg_showmode", "msg_showcmd", "msg_ruler"

    /**
     * Sent when `:messages` command is invoked. History is sent as a list of
     * entries, where each entry is a `[kind, content]` tuple.
     *
     * `["msg_history_show", entries]`
     */
    data class ShowHistory(
        val entries: List<MsgHistoryEntry>,
    ) : MessageEvent

    /**
     * Clear the `:messages` history.
     *
     * `["msg_history_clear"]`
     */
    data object ClearHistory : MessageEvent

    data object Flush : MessageEvent
}

data class MsgChunk(
    val attrId: Int,
    val text: String,
)

data class MsgHistoryEntry(
    val kind: MessageKind,
    val content: List<MsgChunk>,
)
