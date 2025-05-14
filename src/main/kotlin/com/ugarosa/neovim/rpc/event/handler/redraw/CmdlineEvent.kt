package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.cmdline.NeovimCmdlineManager
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent
import com.ugarosa.neovim.rpc.transport.NeovimObject

// :h ui-cmdline
sealed interface CmdlineEvent {
    data class Show(
        val content: List<CmdChunk>,
        val pos: Int,
        val firstChar: String,
        val prompt: String,
        val indent: Int,
        val level: Int,
    ) : CmdlineEvent

    data class Pos(val pos: Int, val level: Int) : CmdlineEvent

    data class SpecialChar(val c: String, val shift: Boolean, val level: Int) : CmdlineEvent

    data class Hide(
        val level: Int,
        val abort: Boolean,
    ) : CmdlineEvent

    data class BlockShow(
        val lines: List<List<CmdChunk>>,
    ) : CmdlineEvent

    data class BlockAppend(
        val line: List<CmdChunk>,
    ) : CmdlineEvent

    data object BlockHide : CmdlineEvent

    data object Flush : CmdlineEvent
}

data class CmdChunk(
    val attrId: Int,
    val text: String,
)

suspend fun onCmdlineEvent(redraw: RedrawEvent) {
    maybeCmdlineEvent(redraw)?.let { event ->
        service<NeovimCmdlineManager>().handleEvent(event)
    }
}

fun maybeCmdlineEvent(redraw: RedrawEvent): CmdlineEvent? {
    return when (redraw.name) {
        "cmdline_show" -> {
            val list = redraw.param.asArray()
            val content = list[0].asArray().map { it.asShowContent() }
            CmdlineEvent.Show(
                content,
                list[1].asInt(),
                list[2].asString(),
                list[3].asString(),
                list[4].asInt(),
                list[5].asInt(),
            )
        }

        "cmdline_pos" -> {
            val list = redraw.param.asArray()
            CmdlineEvent.Pos(
                list[0].asInt(),
                list[1].asInt(),
            )
        }

        "cmdline_special_char" -> {
            val list = redraw.param.asArray()
            CmdlineEvent.SpecialChar(
                list[0].asString(),
                list[1].asBool(),
                list[2].asInt(),
            )
        }

        "cmdline_hide" -> {
            val list = redraw.param.asArray()
            CmdlineEvent.Hide(
                list[0].asInt(),
                list[1].asBool(),
            )
        }

        "cmdline_block_show" -> {
            val list = redraw.param.asArray()
            val lines =
                list[0].asArray().map { line ->
                    line.asArray().map { it.asShowContent() }
                }
            CmdlineEvent.BlockShow(lines)
        }

        "cmdline_block_append" -> {
            val list = redraw.param.asArray()
            val line = list[0].asArray().map { it.asShowContent() }
            CmdlineEvent.BlockAppend(line)
        }

        "cmdline_block_hide" -> {
            CmdlineEvent.BlockHide
        }

        "flush" -> {
            CmdlineEvent.Flush
        }

        else -> null
    }
}

private fun NeovimObject.asShowContent(): CmdChunk {
    val content = asArray()
    return CmdChunk(
        content[0].asInt(),
        content[1].asString(),
    )
}
