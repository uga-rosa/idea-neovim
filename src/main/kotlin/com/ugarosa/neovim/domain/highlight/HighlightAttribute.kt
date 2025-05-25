package com.ugarosa.neovim.domain.highlight

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

data class HighlightAttribute(
    val foreground: Color? = null,
    val background: Color? = null,
    val special: Color? = null,
    val reverse: Boolean = false,
    val italic: Boolean = false,
    val bold: Boolean = false,
    val strikethrough: Boolean = false,
    val underline: Boolean = false,
    val undercurl: Boolean = false,
    val underdouble: Boolean = false,
    val underdotted: Boolean = false,
    val underdashed: Boolean = false,
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): HighlightAttribute {
            return HighlightAttribute(
                foreground = (map["foreground"] as? Int)?.let { newColor(it) },
                background = (map["background"] as? Int)?.let { newColor(it) },
                special = (map["special"] as? Int)?.let { newColor(it) },
                reverse = map["reverse"] as? Boolean ?: false,
                italic = map["italic"] as? Boolean ?: false,
                bold = map["bold"] as? Boolean ?: false,
                strikethrough = map["strikethrough"] as? Boolean ?: false,
                underline = map["underline"] as? Boolean ?: false,
                undercurl = map["undercurl"] as? Boolean ?: false,
                underdouble = map["underdouble"] as? Boolean ?: false,
                underdotted = map["underdotted"] as? Boolean ?: false,
                underdashed = map["underdashed"] as? Boolean ?: false,
            )
        }

        private fun newColor(value: Int): JBColor {
            return JBColor(Color(value), Color(value))
        }
    }

    fun toTextAttributes(): TextAttributes {
        val attrs = TextAttributes()

        foreground?.let { attrs.foregroundColor = it }
        background?.let { attrs.backgroundColor = it }

        // You can merge multiple styles using bitwise OR
        var fontType = Font.PLAIN
        if (bold) fontType = fontType or Font.BOLD
        if (italic) fontType = fontType or Font.ITALIC
        attrs.fontType = fontType

        special?.let { attrs.effectColor = it }

        attrs.effectType =
            when {
                strikethrough -> EffectType.STRIKEOUT
                underline -> EffectType.LINE_UNDERSCORE
                undercurl -> EffectType.WAVE_UNDERSCORE
                underdouble -> EffectType.LINE_UNDERSCORE
                underdotted -> EffectType.BOLD_DOTTED_LINE
                underdashed -> EffectType.BOLD_LINE_UNDERSCORE
                else -> null
            }

        if (reverse) {
            val fg = attrs.foregroundColor
            attrs.foregroundColor = attrs.backgroundColor
            attrs.backgroundColor = fg
        }

        return attrs
    }
}
