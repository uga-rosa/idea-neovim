package com.ugarosa.neovim.highlight

import com.intellij.openapi.components.Service
import com.intellij.ui.JBColor

@Service(Service.Level.APP)
class NeovimHighlightManager {
    private val define = mutableMapOf<Int, HighlightAttribute>()

    init {
        define[0] =
            HighlightAttribute(
                foreground = JBColor.foreground(),
                background = JBColor.background(),
            )
    }

    fun defineAttr(
        id: Int,
        map: Map<String, Any>,
    ) {
        val attr = HighlightAttribute.fromMap(map)
        define[id] = attr
    }

    fun get(id: Int): HighlightAttribute {
        val default = define[0] ?: error("Default highlight not set")
        val highlight = define[id] ?: error("Highlight $id not defined")
        return highlight.copy(
            foreground = highlight.foreground ?: default.foreground,
            background = highlight.background ?: default.background,
        )
    }
}
