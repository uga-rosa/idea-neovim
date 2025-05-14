package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.cmdline.CmdChunk
import com.ugarosa.neovim.cmdline.CmdlineEvent
import com.ugarosa.neovim.cmdline.NeovimCmdlineManager
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent
import com.ugarosa.neovim.rpc.transport.NeovimObject

suspend fun onCmdlineEvent(redraw: RedrawEvent) {
    maybeCmdlineEvent(redraw)?.let { event ->
        service<NeovimCmdlineManager>().handleEvent(event)
    }
}

fun maybeCmdlineEvent(redraw: RedrawEvent): CmdlineEvent? {
    return when (redraw.name) {
        "cmdline_show" -> {
            val content = redraw.param[0].asArray().map { it.asShowContent() }
            CmdlineEvent.Show(
                content,
                redraw.param[1].asInt(),
                redraw.param[2].asString(),
                redraw.param[3].asString(),
                redraw.param[4].asInt(),
                redraw.param[5].asInt(),
            )
        }

        "cmdline_pos" -> {
            CmdlineEvent.Pos(
                redraw.param[0].asInt(),
                redraw.param[1].asInt(),
            )
        }

        "cmdline_special_char" -> {
            CmdlineEvent.SpecialChar(
                redraw.param[0].asString(),
                redraw.param[1].asBool(),
                redraw.param[2].asInt(),
            )
        }

        "cmdline_hide" -> {
            CmdlineEvent.Hide(
                redraw.param[0].asInt(),
                redraw.param[1].asBool(),
            )
        }

        "cmdline_block_show" -> {
            val lines =
                redraw.param[0].asArray().map { line ->
                    line.asArray().map { it.asShowContent() }
                }
            CmdlineEvent.BlockShow(lines)
        }

        "cmdline_block_append" -> {
            val line = redraw.param[0].asArray().map { it.asShowContent() }
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
