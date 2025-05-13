package com.ugarosa.neovim.rpc.transport

sealed interface RpcMessage {
    data class Response(
        val id: Int,
        val result: NeovimObject,
        val error: NeovimObject,
    ) : RpcMessage

    data class Notification(
        val method: String,
        val params: List<NeovimObject>,
    ) : RpcMessage
}
