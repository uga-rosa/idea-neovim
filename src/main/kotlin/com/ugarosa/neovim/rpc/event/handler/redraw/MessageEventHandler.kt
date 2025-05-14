package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.message.MessageEvent
import com.ugarosa.neovim.message.MessageKind
import com.ugarosa.neovim.message.MsgChunk
import com.ugarosa.neovim.message.MsgHistoryEntry
import com.ugarosa.neovim.message.NeovimMessageManager
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent

suspend fun onMessageEvent(redraw: RedrawEvent) {
    maybeMessageEvent(redraw)?.let { event ->
        service<NeovimMessageManager>().handleMessageEvent(event)
    }
}

fun maybeMessageEvent(redraw: RedrawEvent): MessageEvent? {
    return when (redraw.name) {
        "msg_show" -> {
            val list = redraw.param.asArray()
            val kind = MessageKind.fromValue(list[0].asString())
            val content =
                list[1].asArray().map {
                    val chunk = it.asArray()
                    MsgChunk(chunk[0].asInt(), chunk[1].asString())
                }
            val replaceLast = list[2].asBool()
            val history = list[3].asBool()
            MessageEvent.Show(kind, content, replaceLast, history)
        }

        "msg_clear" -> MessageEvent.Clear

        "msg_history_show" -> {
            val entries =
                redraw.param.asArray().map { entry ->
                    val list = entry.asArray()
                    val kind = MessageKind.fromValue(list[0].asString())
                    val content =
                        list[1].asArray().map {
                            MsgChunk(it.asInt(), it.asString())
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
