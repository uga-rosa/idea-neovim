package com.ugarosa.neovim.adapter.nvim.incoming.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.domain.highlight.NvimHighlightManager
import com.ugarosa.neovim.rpc.transport.NvimObject

fun onHighlightEvent(
    name: String,
    param: List<NvimObject>,
) {
    val highlightManager = service<NvimHighlightManager>()
    when (name) {
        "default_colors_set" -> {
            // Ignore this event since we want to use IDE default colors
        }

        "hl_attr_define" -> {
            val id = param[0].asInt()
            val attr = param[1].asStringMap()
            highlightManager.defineAttr(id, attr)
        }
    }
}
