package com.ugarosa.neovim.rpc.event.handler.redraw

import com.intellij.openapi.components.service
import com.ugarosa.neovim.highlight.NeovimHighlightManager
import com.ugarosa.neovim.rpc.event.handler.RedrawEvent

fun onSetHighlight(redraw: RedrawEvent) {
    val highlightManager = service<NeovimHighlightManager>()
    when (redraw.name) {
        "default_colors_set" -> {
            // Ignore this event since we want to use IDE default colors
        }

        "hl_attr_define" -> {
            val list = redraw.param.asArray()
            val id = list[0].asInt()
            val attr = list[1].asStringMap()
            highlightManager.defineAttr(id, attr)
        }
    }
}
