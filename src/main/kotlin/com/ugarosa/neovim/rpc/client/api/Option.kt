package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.rpc.client.NvimClient

suspend fun NvimClient.getGlobalOption(): Map<String, Any> {
    return execLua("option", "get_global").asStringMap()
}

suspend fun NvimClient.getLocalOption(bufferId: BufferId): Map<String, Any> {
    return execLua("option", "get_local", listOf(bufferId)).asStringMap()
}

suspend fun NvimClient.setFiletype(
    bufferId: BufferId,
    path: String,
) {
    execLua("option", "set_filetype", listOf(bufferId, path))
}

suspend fun NvimClient.modifiable(bufferId: BufferId) {
    execLua("option", "set_writable", listOf(bufferId))
}

suspend fun NvimClient.noModifiable(bufferId: BufferId) {
    execLua("option", "set_no_writable", listOf(bufferId))
}
