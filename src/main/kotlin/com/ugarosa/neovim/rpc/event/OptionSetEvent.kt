package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

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

fun maybeOptionSetEvent(push: NeovimRpcClient.PushNotification): OptionSetEvent? {
    if (push.method != "nvim_option_set_event") {
        return null
    }
    try {
        val params = push.params.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val scope = params[1].asStringValue().asString().let { OptionScope.fromRaw(it) }
        val name = params[2].asStringValue().asString()
        val value = params[3].asAny()
        return OptionSetEvent(bufferId, scope, name, value)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
