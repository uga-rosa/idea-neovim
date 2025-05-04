package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.client.NeovimRpcClient

enum class NeovimMode {
    NORMAL,
    VISUAL,
    VISUAL_LINE,
    VISUAL_BLOCK,
    SELECT,
    INSERT,
    REPLACE,
    COMMAND,
    OTHER,
}

suspend fun getMode(client: NeovimRpcClient): Either<NeovimFunctionsError, NeovimMode> =
    either {
        val response =
            client.requestAsync("nvim_get_mode")
                .mapLeft { it.translate() }.bind()
        val modeString = response.result.asMapValue().get("mode")?.asStringValue()?.asString()
        when (modeString?.get(0)) {
            'n' -> NeovimMode.NORMAL
            'v' -> NeovimMode.VISUAL
            'V' -> NeovimMode.VISUAL_LINE
            '\u0016' -> NeovimMode.VISUAL_BLOCK
            's' -> NeovimMode.SELECT
            'i' -> NeovimMode.INSERT
            'R' -> NeovimMode.REPLACE
            'c' -> NeovimMode.COMMAND
            else -> NeovimMode.OTHER
        }
    }
