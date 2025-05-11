package com.ugarosa.neovim.rpc.function

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.ugarosa.neovim.common.asStringMap
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun addRuntimePath(client: NeovimRpcClient) {
    val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.ugarosa.neovim"))!!
    val rtpDir = plugin.pluginPath.resolve("runtime").toAbsolutePath().toString()
    client.notify("nvim_command", listOf("set rtp+=$rtpDir"))
}

suspend fun getGlobalOptions(client: NeovimRpcClient): Map<String, Any>? =
    execLua(client, "option", "get_global")
        ?.decode { it.asStringMap() }

suspend fun getLocalOptions(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Map<String, Any>? =
    execLua(client, "option", "get_local", listOf(bufferId))
        ?.decode { it.asStringMap() }
