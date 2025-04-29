package com.ugarosa.neovim.service

import com.intellij.ui.JBColor
import com.ugarosa.neovim.infra.NeovimRpcClient

object NeovimFunctions {
    fun createBuffer(rpcClient: NeovimRpcClient): BufferId {
        val response = rpcClient.requestSync("nvim_create_buf", listOf(true, false), null)
        return response.result.asBufferId()
    }

    fun bufferSetLines(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
        start: Int,
        end: Int,
        lines: List<String>,
    ) {
        rpcClient.requestSync(
            "nvim_buf_set_lines",
            listOf(bufferId, start, end, false, lines),
        )
    }

    fun bufferAttach(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestSync(
            "nvim_buf_attach",
            listOf(bufferId, true, emptyMap<String, Any>()),
        )
    }

    fun setCurrentBuffer(
        rpcClient: NeovimRpcClient,
        bufferId: BufferId,
    ) {
        rpcClient.requestSync("nvim_set_current_buf", listOf(bufferId))
    }

    fun input(
        rpcClient: NeovimRpcClient,
        key: String,
    ) {
        rpcClient.requestSync("nvim_input", listOf(key))
    }

    fun getCursor(rpcClient: NeovimRpcClient): Pair<Int, Int> {
        val response = rpcClient.requestSync("nvim_win_get_cursor", listOf(0))
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

    fun getMode(rpcClient: NeovimRpcClient): NeovimMode {
        val response = rpcClient.requestSync("nvim_get_mode")
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
