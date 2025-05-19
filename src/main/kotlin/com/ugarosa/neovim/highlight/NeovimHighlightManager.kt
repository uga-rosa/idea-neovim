package com.ugarosa.neovim.highlight

import com.intellij.openapi.components.Service
import com.intellij.ui.JBColor

@Service(Service.Level.APP)
class NeovimHighlightManager {
    val defaultForeground = JBColor.foreground()
    val defaultBackground = JBColor.background()

    private val define = mutableMapOf<Int, HighlightAttribute>()

    init {
        define[0] =
            HighlightAttribute(
                foreground = defaultForeground,
                background = defaultBackground,
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
        val highlight = define[attrId] ?: error("Highlight $attrId not defined")
        return highlight.copy(
            foreground = highlight.foreground ?: defaultForeground,
            background = highlight.background ?: defaultBackground,
        )
    }
}
