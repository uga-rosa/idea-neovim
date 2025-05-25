package com.ugarosa.neovim.adapter.idea.ui.cmdline

// :h ui-cmdline
sealed interface CmdlineEvent {
    data class Show(
        val content: List<CmdChunk>,
        val pos: Int,
        val firstChar: String,
        val prompt: String,
        val indent: Int,
        val level: Int,
        // used for prompt
        val hlId: Int,
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
