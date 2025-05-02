package com.ugarosa.neovim.rpc

import com.intellij.ui.JBColor
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.jvm.Throws

object NeovimFunctions {
    @Throws(TimeoutCancellationException::class)
    suspend fun createBuffer(rpcClient: NeovimRpcClient): BufferId {
        val response = rpcClient.requestAsync("nvim_create_buf", listOf(true, false))
        return response.result.asBufferId()
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun bufferSetLines(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
        start: Int,
        end: Int,
        lines: List<String>,
    ) {
        rpcClient.requestAsync(
            "nvim_buf_set_lines",
            listOf(bufferId, start, end, false, lines),
        )
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun bufferAttach(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestAsync(
            "nvim_buf_attach",
            listOf(bufferId, false, emptyMap<String, Any>()),
        )
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun bufferDetach(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestAsync(
            "nvim_buf_detach",
            listOf(bufferId),
        )
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun setCurrentBuffer(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestAsync("nvim_set_current_buf", listOf(bufferId))
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun input(
        rpcClient: NeovimRpcClient,
        key: String,
    ) {
        rpcClient.requestAsync("nvim_input", listOf(key))
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun getCursor(rpcClient: NeovimRpcClient): Pair<Int, Int> {
        val response = rpcClient.requestAsync("nvim_win_get_cursor", listOf(0))
        val cursorArray = response.result.asArrayValue().list()
        val row = cursorArray[0].asIntegerValue().toInt()
        val col = cursorArray[1].asIntegerValue().toInt()
        return row to col
    }

    fun maybeBufLinesEvent(push: NeovimRpcClient.PushNotification): BufLinesEvent? {
        if (push.method != "nvim_buf_lines_event") {
            return null
        }
        val params = push.params.asArrayValue().list()
        val bufferId = params[0].asBufferId()
        val firstLine = params[2].asIntegerValue().toInt()
        val lastLine = params[3].asIntegerValue().toInt()
        val replacementLines = params[4].asArrayValue().list().map { it.asStringValue().asString() }
        return BufLinesEvent(bufferId, firstLine, lastLine, replacementLines)
    }

    @Throws(TimeoutCancellationException::class)
    suspend fun getMode(rpcClient: NeovimRpcClient): NeovimMode {
        val response = rpcClient.requestAsync("nvim_get_mode")
        val modeString = response.result.asMapValue().get("mode")?.asStringValue()?.asString()
        return when (modeString?.get(0)) {
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
}

data class BufLinesEvent(
    val bufferId: BufferId,
    val firstLine: Int,
    val lastLine: Int,
    val replacementLines: List<String>,
)

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
