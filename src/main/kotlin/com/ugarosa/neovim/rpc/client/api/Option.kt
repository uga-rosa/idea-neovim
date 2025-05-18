package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.buffer.BufferId
import com.ugarosa.neovim.rpc.client.NeovimClient

suspend fun NeovimClient.getGlobalOption(): Map<String, Any> {
    return execLua("option", "get_global").asStringMap()
}

suspend fun NeovimClient.getLocalOption(bufferId: BufferId): Map<String, Any> {
    return execLua("option", "get_local", listOf(bufferId)).asStringMap()
}

suspend fun NeovimClient.setFiletype(
    bufferId: BufferId,
    path: String,
) {
    execLua("option", "set_filetype", listOf(bufferId, path))
}

suspend fun NeovimClient.modifiable(bufferId: BufferId) {
    execLua("option", "set_writable", listOf(bufferId))
}

suspend fun NeovimClient.noModifiable(bufferId: BufferId) {
    execLua("option", "set_no_writable", listOf(bufferId))
}
