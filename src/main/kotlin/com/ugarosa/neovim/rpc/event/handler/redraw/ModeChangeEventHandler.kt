package com.ugarosa.neovim.rpc.event.handler.redraw

import com.ugarosa.neovim.buffer.NeovimBufferManager
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.mode.NeovimMode
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent

suspend fun onModeChangeEvent(redraw: RedrawEvent) {
    when (redraw.name) {
        "mode_change" -> {
            val mode = redraw.param[0].asString().let { NeovimMode.fromModeChangeEvent(it) } ?: return

            val editor = focusEditor() ?: return
            val buffer = NeovimBufferManager.findByEditor(editor)
            buffer.handleModeChangeEvent(mode)
        }
    }
}
