package com.ugarosa.neovim.rpc.client.api

import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.type.NeovimObject

suspend fun NeovimClient.getGlobalOption(): Map<String, Any> {
    val resp = execLua("option", "get_global")
    return (resp as NeovimObject.Dict).map.mapValues { it.value.asAny() }
}

suspend fun NeovimClient.getLocalOption(bufferId: NeovimObject.BufferId): Map<String, Any> {
    val resp = execLua("option", "get_local", listOf(bufferId))
    return (resp as NeovimObject.Dict).map.mapValues { it.value.asAny() }
}

suspend fun NeovimClient.setFiletype(
    bufferId: NeovimObject.BufferId,
    path: String,
) {
    execLuaNotify("option", "set_filetype", listOf(bufferId, path))
}

suspend fun NeovimClient.modifiable(bufferId: NeovimObject.BufferId) {
    execLuaNotify("option", "set_writable", listOf(bufferId))
}

suspend fun NeovimClient.noModifiable(bufferId: NeovimObject.BufferId) {
    execLuaNotify("option", "set_no_writable", listOf(bufferId))
}
