package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.common.asAny
import com.ugarosa.neovim.common.decode
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import com.ugarosa.neovim.rpc.function.ChanIdManager
import com.ugarosa.neovim.rpc.function.execLuaNotify
import com.ugarosa.neovim.rpc.function.readLuaCode

data class OptionSetEvent(
    val bufferId: BufferId,
    val scope: OptionScope,
    val name: String,
    val value: Any,
)

enum class OptionScope {
    GLOBAL,
    LOCAL,
    ;

    companion object {
        fun fromRaw(raw: String): OptionScope {
            return when (raw) {
                "global" -> GLOBAL
                "local" -> LOCAL
                else -> throw IllegalArgumentException("Invalid value for OptionScope: $raw")
            }
        }
    }
}

suspend fun hookGlobalOptionSet(client: NeovimRpcClient) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookGlobalOptionSet.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId))
}

suspend fun hookLocalOptionSet(
    client: NeovimRpcClient,
    bufferId: BufferId,
) {
    val chanId = ChanIdManager.fetch(client)
    val luaCode = readLuaCode("/lua/hookLocalOptionSet.lua") ?: return
    execLuaNotify(client, luaCode, listOf(chanId, bufferId))
}

fun maybeOptionSetEvent(push: NeovimRpcClient.PushNotification): OptionSetEvent? {
    if (push.method != "nvim_option_set_event") {
        return null
    }
    return push.params.decode {
        val params = it.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val scope = params[1].asStringValue().asString().let { OptionScope.fromRaw(it) }
        val name = params[2].asStringValue().asString()
        val value = params[3].asAny()
        OptionSetEvent(bufferId, scope, name, value)
    }
}
