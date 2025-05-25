package com.ugarosa.neovim.rpc.transport

sealed interface RpcMessage {
    data class Response(
        val id: Int,
        val result: NvimObject,
        val error: NvimObject,
    ) : RpcMessage

    data class Notification(
        val method: String,
        val params: List<NvimObject>,
    ) : RpcMessage
}
