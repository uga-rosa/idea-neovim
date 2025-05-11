package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.common.asStringMap
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

suspend fun getGlobalOptions(client: NeovimRpcClient): Map<String, Any>? =
    execLua(client, "option", "get_global")
        ?.decode { it.asStringMap() }

suspend fun getLocalOptions(
    client: NeovimRpcClient,
    bufferId: BufferId,
): Map<String, Any>? =
    execLua(client, "option", "get_local", listOf(bufferId))
        ?.decode { it.asStringMap() }
