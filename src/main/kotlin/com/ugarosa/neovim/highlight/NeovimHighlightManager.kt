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
        attrId: Int,
        map: Map<String, Any>,
    ) {
        val attr = HighlightAttribute.fromMap(map)
        define[attrId] = attr
    }

    fun get(attrId: Int): HighlightAttribute {
        val default = define[0] ?: error("Default highlight not set")
        val highlight = define[attrId] ?: error("Highlight $attrId not defined")
        return highlight.copy(
            foreground = highlight.foreground ?: default.foreground,
            background = highlight.background ?: default.background,
        )
    }
}
