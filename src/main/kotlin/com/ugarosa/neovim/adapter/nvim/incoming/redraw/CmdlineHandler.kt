package com.ugarosa.neovim.adapter.nvim.incoming.redraw

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.ugarosa.neovim.adapter.idea.ui.cmdline.CmdChunk
import com.ugarosa.neovim.adapter.idea.ui.cmdline.CmdlineEvent
import com.ugarosa.neovim.adapter.idea.ui.cmdline.NvimCmdlineManager
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.rpc.transport.NvimObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun onCmdlineEvent(
    name: String,
    param: List<NvimObject>,
) {
    maybeCmdlineEvent(name, param)?.let { event ->
        val cmdlineManager =
            withContext(Dispatchers.EDT) {
                focusProject()?.service<NvimCmdlineManager>()
            } ?: return
        cmdlineManager.handleEvent(event)
    }
}

private fun maybeCmdlineEvent(
    name: String,
    param: List<NvimObject>,
): CmdlineEvent? {
    return when (name) {
        "cmdline_show" -> {
            val content = param[0].asArray().map { it.asShowContent() }
            CmdlineEvent.Show(
                content,
                param[1].asInt(),
                param[2].asString(),
                param[3].asString(),
                param[4].asInt(),
                param[5].asInt(),
                param[6].asInt(),
            )
        }

        "cmdline_pos" -> {
            CmdlineEvent.Pos(
                param[0].asInt(),
                param[1].asInt(),
            )
        }

        "cmdline_special_char" -> {
            CmdlineEvent.SpecialChar(
                param[0].asString(),
                param[1].asBool(),
                param[2].asInt(),
            )
        }

        "cmdline_hide" -> {
            CmdlineEvent.Hide(
                param[0].asInt(),
                param[1].asBool(),
            )
        }

        "cmdline_block_show" -> {
            val lines =
                param[0].asArray().map { line ->
                    line.asArray().map { it.asShowContent() }
                }
            CmdlineEvent.BlockShow(lines)
        }

        "cmdline_block_append" -> {
            val line = param[0].asArray().map { it.asShowContent() }
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

private fun NvimObject.asShowContent(): CmdChunk {
    val content = asArray()
    return CmdChunk(
        content[0].asInt(),
        content[1].asString(),
    )
}
