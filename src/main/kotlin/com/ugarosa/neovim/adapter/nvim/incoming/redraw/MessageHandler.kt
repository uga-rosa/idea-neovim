package com.ugarosa.neovim.adapter.nvim.incoming.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.adapter.idea.ui.message.MessageEvent
import com.ugarosa.neovim.adapter.idea.ui.message.MessageKind
import com.ugarosa.neovim.adapter.idea.ui.message.MsgChunk
import com.ugarosa.neovim.adapter.idea.ui.message.MsgHistoryEntry
import com.ugarosa.neovim.adapter.idea.ui.message.NvimMessageManager
import com.ugarosa.neovim.rpc.transport.NvimObject

suspend fun onMessageEvent(
    name: String,
    param: List<NvimObject>,
) {
    maybeMessageEvent(name, param)?.let { event ->
        service<NvimMessageManager>().handleMessageEvent(event)
    }
}

private fun maybeMessageEvent(
    name: String,
    param: List<NvimObject>,
): MessageEvent? {
    return when (name) {
        "msg_show" -> {
            val kind = MessageKind.fromValue(param[0].asString())
            val content =
                param[1].asArray().map {
                    val chunk = it.asArray()
                    MsgChunk(chunk[0].asInt(), chunk[1].asString())
                }
            val replaceLast = param[2].asBool()
            val history = param[3].asBool()
            MessageEvent.Show(kind, content, replaceLast, history)
        }

        "msg_clear" -> MessageEvent.Clear

        "msg_history_show" -> {
            val entries =
                param.map { entry ->
                    val list = entry.asArray()[0].asArray()
                    val kind = MessageKind.fromValue(list[0].asString())
                    val content =
                        list[1].asArray().map {
                            val chunk = it.asArray()
                            MsgChunk(chunk[0].asInt(), chunk[1].asString())
                        }
                    MsgHistoryEntry(kind, content)
                }
            MessageEvent.ShowHistory(entries)
        }

        "msg_history_clear" -> MessageEvent.ClearHistory

        "flush" -> MessageEvent.Flush

        else -> null
    }
}
