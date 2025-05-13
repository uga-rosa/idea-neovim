package com.ugarosa.neovim.rpc.event.handler.redraw

import com.ugarosa.neovim.rpc.event.handler.RedrawEvent
import com.ugarosa.neovim.rpc.transport.NeovimObject

/**
 * Cmdline Events							   *ui-cmdline*
 *
 * Activated by the `ext_cmdline` |ui-option|.
 *
 * This UI extension delegates presentation of the |cmdline| (except 'wildmenu').
 * For command-line 'wildmenu' UI events, activate |ui-popupmenu|.
 */
sealed interface CmdlineEvent {
    /**
     * ["cmdline_show", content, pos, firstc, prompt, indent, level, hl_id] ~
     *         content: List of [attrs, string]
     * 	         [[{}, "t"], [attrs, "est"], ...]
     *
     * 	Triggered when the cmdline is displayed or changed.
     * 	The `content` is the full content that should be displayed in the
     * 	cmdline, and the `pos` is the position of the cursor that in the
     * 	cmdline. The content is divided into chunks with different highlight
     * 	attributes represented as a dict (see |ui-event-highlight_set|).
     *
     * 	`firstc` and `prompt` are text, that if non-empty should be
     * 	displayed in front of the command line. `firstc` always indicates
     * 	built-in command lines such as `:` (ex command) and `/` `?` (search),
     * 	while `prompt` is an |input()| prompt, highlighted with `hl_id`.
     * 	`indent` tells how many spaces the content should be indented.
     *
     * 	The Nvim command line can be invoked recursively, for instance by
     * 	typing `<c-r>=` at the command line prompt. The `level` field is used
     * 	to distinguish different command lines active at the same time. The
     * 	first invoked command line has level 1, the next recursively-invoked
     * 	prompt has level 2. A command line invoked from the |cmdline-window|
     * 	has a higher level than the edited command line.
     */
    data class Show(
        val content: List<ShowChunk>,
        val pos: Int,
        val firstChar: String,
        val prompt: String,
        val indent: Int,
        val level: Int,
    ) : CmdlineEvent

    data class ShowChunk(
        val highlightAttributes: HighlightAttributes,
        val text: String,
    )

    /**
     * ["cmdline_pos", pos, level] ~
     * 	Change the cursor position in the cmdline.
     */
    data class Pos(val pos: Int, val level: Int) : CmdlineEvent

    /**
     * ["cmdline_special_char", c, shift, level] ~
     * 	Display a special char in the cmdline at the cursor position. This is
     * 	typically used to indicate a pending state, e.g. after |c_CTRL-V|. If
     * 	`shift` is true the text after the cursor should be shifted, otherwise
     * 	it should overwrite the char at the cursor.
     *
     * 	Should be hidden at next cmdline_show.
     */
    data class SpecialChar(val c: String, val shift: Boolean, val level: Int) : CmdlineEvent

    /**
     * INFO: This help is wrong. "cmdline_hide" has two parameters: level and abort.
     *
     * ["cmdline_hide", abort] ~
     * 	Hide the cmdline. `abort` is true if the cmdline is hidden after an
     * 	aborting condition (|c_Esc| or |c_CTRL-C|).
     */
    data class Hide(
        val level: Int,
        val abort: Boolean,
    ) : CmdlineEvent

    /**
     * ["cmdline_block_show", lines] ~
     * 	Show a block of context to the current command line. For example if
     * 	the user defines a |:function| interactively: >vim
     * 	    :function Foo()
     * 	    :  echo "foo"
     * 	    :
     * <
     * 	`lines` is a list of lines of highlighted chunks, in the same form as
     * 	the "cmdline_show" `contents` parameter.
     */
    data class BlockShow(
        val lines: List<List<ShowChunk>>,
    ) : CmdlineEvent

    /**
     * ["cmdline_block_append", line] ~
     * 	Append a line at the end of the currently shown block.
     */
    data class BlockAppend(
        val line: List<ShowChunk>,
    ) : CmdlineEvent

    /**
     * ["cmdline_block_hide"] ~
     * 	Hide the block.
     */
    data object BlockHide : CmdlineEvent

    /**
     * This is a global event.
     *
     * ["flush"]
     * 	Nvim is done redrawing the screen. For an implementation that renders
     * 	to an internal buffer, this is the time to display the redrawn parts
     * 	to the user.
     */
    data object Flush : CmdlineEvent
}

private fun NeovimObject.asShowContent(): CmdlineEvent.ShowChunk {
    val content = asArray()
    return CmdlineEvent.ShowChunk(
        HighlightAttributes.Companion.fromMap(content[0].asStringMap()),
        content[1].asString(),
    )
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
