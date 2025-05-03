package com.ugarosa.neovim.rpc

import arrow.core.Either
import arrow.core.raise.either
import com.intellij.ui.JBColor
import com.ugarosa.neovim.common.get
import com.ugarosa.neovim.rpc.client.NeovimClient
import com.ugarosa.neovim.rpc.msgpack.BufferId
import com.ugarosa.neovim.rpc.msgpack.asBufferId

sealed interface NeovimFunctionsError {
    data object Timeout : NeovimFunctionsError

    data object IO : NeovimFunctionsError

    data object BadRequest : NeovimFunctionsError

    data object ResponseTypeMismatch : NeovimFunctionsError

    data object Unexpected : NeovimFunctionsError
}

private fun NeovimClient.RequestError.translate(): NeovimFunctionsError {
    return when (this) {
        is NeovimClient.RequestError.BadRequest -> NeovimFunctionsError.BadRequest
        is NeovimClient.RequestError.IO -> NeovimFunctionsError.IO
        is NeovimClient.RequestError.Timeout -> NeovimFunctionsError.Timeout
        is NeovimClient.RequestError.Unexpected -> NeovimFunctionsError.Unexpected
    }
}

suspend fun createBuffer(client: NeovimClient): Either<NeovimFunctionsError, BufferId> =
    either {
        val response =
            client.requestAsync("nvim_create_buf", listOf(true, false))
                .mapLeft { it.translate() }.bind()
        response.result.asBufferId()
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
    }

suspend fun bufferSetLines(
    client: NeovimClient,
    bufferId: BufferId,
    start: Int,
    end: Int,
    lines: List<String>,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_buf_set_lines",
            listOf(bufferId, start, end, false, lines),
        )
            .onLeft { raise(it.translate()) }
    }

suspend fun bufferAttach(
    client: NeovimClient,
    bufferId: BufferId,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_buf_attach",
            listOf(bufferId, false, emptyMap<String, Any>()),
        )
            .onLeft { raise(it.translate()) }
    }

suspend fun bufferDetach(
    client: NeovimClient,
    bufferId: BufferId,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_buf_detach",
            listOf(bufferId),
        )
            .onLeft { raise(it.translate()) }
    }

suspend fun execLua(
    client: NeovimClient,
    code: String,
    args: List<Any> = emptyList(),
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_exec_lua",
            listOf(code, args),
        )
            .onLeft { raise(it.translate()) }
    }

suspend fun getChanId(client: NeovimClient): Either<NeovimFunctionsError, Int> =
    either {
        val response =
            client.requestAsync("nvim_get_chan_info", listOf(0))
                .mapLeft { it.translate() }.bind()
        Either.catch {
            response.result.asMapValue().get("id")?.asIntegerValue()?.toInt()!!
        }
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
    }

suspend fun setCurrentBuffer(
    client: NeovimClient,
    bufferId: BufferId,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync("nvim_set_current_buf", listOf(bufferId))
            .onLeft { raise(it.translate()) }
    }

suspend fun input(
    client: NeovimClient,
    key: String,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync("nvim_input", listOf(key))
            .onLeft { raise(it.translate()) }
    }

suspend fun getCursor(client: NeovimClient): Either<NeovimFunctionsError, Pair<Int, Int>> =
    either {
        val response =
            client.requestAsync("nvim_win_get_cursor", listOf(0))
                .mapLeft { it.translate() }.bind()
        Either.catch {
            val cursorArray = response.result.asArrayValue().list()
            val row = cursorArray[0].asIntegerValue().toInt()
            val col = cursorArray[1].asIntegerValue().toInt()
            row to col
        }
            .mapLeft { NeovimFunctionsError.ResponseTypeMismatch }.bind()
    }

suspend fun setCursor(
    client: NeovimClient,
    pos: Pair<Int, Int>,
): Either<NeovimFunctionsError, Unit> =
    either {
        client.requestAsync(
            "nvim_win_set_cursor",
            listOf(0, listOf(pos.first, pos.second)),
        )
            .onLeft { raise(it.translate()) }
    }

suspend fun getMode(client: NeovimClient): Either<NeovimFunctionsError, NeovimMode> =
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

enum class NeovimMode(val color: JBColor) {
    NORMAL(JBColor.GREEN),
    VISUAL(JBColor.BLUE),
    VISUAL_LINE(JBColor.BLUE),
    VISUAL_BLOCK(JBColor.BLUE),
    SELECT(JBColor.BLUE),
    INSERT(JBColor.YELLOW),
    REPLACE(JBColor.ORANGE),
    COMMAND(JBColor.GREEN),
    OTHER(JBColor.RED),
}
