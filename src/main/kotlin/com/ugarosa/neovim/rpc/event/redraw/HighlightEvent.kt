package com.ugarosa.neovim.rpc.event.redraw

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

data class HighlightAttributes(
    val foreground: String? = null,
    val background: String? = null,
    val special: String? = null,
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
        fun fromMap(map: Map<String, Any?>): HighlightAttributes {
            return HighlightAttributes(
                foreground = map["foreground"] as? String,
                background = map["background"] as? String,
                special = map["special"] as? String,
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
    }

    fun toTextAttributes(): TextAttributes {
        val fg = foreground?.let { parseColor(it) }
        val bg = background?.let { parseColor(it) }

        val effectColor = special?.let { parseColor(it) }

        val effectType =
            when {
                underline -> EffectType.LINE_UNDERSCORE
                strikethrough -> EffectType.STRIKEOUT
                undercurl -> EffectType.WAVE_UNDERSCORE
                underdouble -> EffectType.BOLD_LINE_UNDERSCORE
                underdotted -> EffectType.BOXED
                underdashed -> EffectType.BOLD_DOTTED_LINE
                else -> null
            }

        val fontType =
            when {
                bold && italic -> Font.BOLD or Font.ITALIC
                bold -> Font.BOLD
                italic -> Font.ITALIC
                else -> Font.PLAIN
            }

        return TextAttributes(
            fg,
            bg,
            effectColor,
            effectType,
            fontType,
        )
    }

    private fun parseColor(hex: String): JBColor {
        val color =
            try {
                Color.decode(hex)
            } catch (e: Exception) {
                JBColor.foreground()
            }
        return JBColor(color, color)
    }
}
