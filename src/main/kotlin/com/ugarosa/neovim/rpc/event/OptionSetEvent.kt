package com.ugarosa.neovim.rpc.event

import com.intellij.openapi.diagnostic.Logger
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

private val logger = Logger.getInstance("com.ugarosa.neovim.rpc.event")

data class OptionSetEvent(
    val name: String,
    val scope: OptionScope,
    val value: Any,
    val bufferId: BufferId,
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
    if (push.method != "nvim_option_set") {
        return null
    }
    try {
        val params = push.params.asMapValue()
        val name =
            params.get("name")?.asStringValue()?.asString()
                ?: throw IllegalArgumentException("Invalid name: ${params.get("name")}")
        val scope =
            params.get("scope")?.asStringValue()?.asString()
                ?.let { OptionScope.fromRaw(it) }
                ?: throw IllegalArgumentException("Invalid scope: ${params.get("scope")}")
        val value =
            params.get("value")?.asAny()
                ?: throw IllegalArgumentException("Invalid value: ${params.get("value")}")
        val bufferId =
            params.get("buffer")?.asIntegerValue()?.toInt()
                ?.let { BufferId(it) }
                ?: throw IllegalArgumentException("Invalid buffer ID: ${params.get("buffer")}")
        return OptionSetEvent(name, scope, value, bufferId)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
