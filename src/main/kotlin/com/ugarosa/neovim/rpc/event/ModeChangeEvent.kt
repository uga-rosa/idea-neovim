package com.ugarosa.neovim.rpc.event

import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

data class ModeChangeEvent(
    val bufferId: BufferId,
    val mode: NeovimMode,
)

enum class NeovimModeKind {
    NORMAL,
    VISUAL,
    VISUAL_LINE,
    VISUAL_BLOCK,
    SELECT,
    SELECT_LINE,
    SELECT_BLOCK,
    INSERT,
    REPLACE,
    COMMAND,
    OTHER,
}

data class NeovimMode(
    val kind: NeovimModeKind,
    val raw: String,
) {
    companion object {
        fun fromRaw(raw: String): NeovimMode {
            val kind =
                when (raw[0]) {
                    'n' -> NeovimModeKind.NORMAL
                    'v' -> NeovimModeKind.VISUAL
                    'V' -> NeovimModeKind.VISUAL_LINE
                    '\u0016' -> NeovimModeKind.VISUAL_BLOCK // Ctrl-V
                    's' -> NeovimModeKind.SELECT
                    'S' -> NeovimModeKind.SELECT_LINE
                    '\u0013' -> NeovimModeKind.SELECT_BLOCK // Ctrl-S
                    'i' -> NeovimModeKind.INSERT
                    'R' -> NeovimModeKind.REPLACE
                    'c' -> NeovimModeKind.COMMAND
                    else -> NeovimModeKind.OTHER
                }
            return NeovimMode(kind, raw)
        }

        val default = NeovimMode(NeovimModeKind.NORMAL, "n")
    }
}

fun maybeModeChangeEvent(push: NeovimRpcClient.PushNotification): ModeChangeEvent? {
    if (push.method != "nvim_mode_change") {
        return null
    }
    try {
        val params = push.params.asArrayValue().list()
        val bufferId = params[0].asIntegerValue().toInt().let { BufferId(it) }
        val mode = params[1].asStringValue().asString().let { NeovimMode.fromRaw(it) }
        return ModeChangeEvent(bufferId, mode)
    } catch (e: Exception) {
        logger.warn(e)
        return null
    }
}
