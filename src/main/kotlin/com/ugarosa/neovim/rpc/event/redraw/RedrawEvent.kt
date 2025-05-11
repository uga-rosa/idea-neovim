package com.ugarosa.neovim.rpc.event.redraw

import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import org.msgpack.value.Value

data class RedrawEvent(
    val name: String,
    val param: Value,
)

fun maybeRedrawEvent(push: NeovimRpcClient.PushNotification): List<RedrawEvent>? {
    if (push.method != "redraw") return null

    return push.params.decode { value ->
        value.asArrayValue().list().map {
            val event = it.asArrayValue().list()
            RedrawEvent(
                event[0].asStringValue().asString(),
                event[1],
            )
        }
    }
}
