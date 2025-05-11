package com.ugarosa.neovim.rpc.event.redraw

import java.awt.Color
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

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

    fun toAttributeSet(): AttributeSet =
        SimpleAttributeSet().apply {
            foreground?.let { StyleConstants.setForeground(this, Color.decode(it)) }
            background?.let { StyleConstants.setBackground(this, Color.decode(it)) }
            // Don't support special (color to use for various underlines, when present).
            if (reverse) {
                val fg = StyleConstants.getForeground(this)
                val bg = StyleConstants.getBackground(this)
                StyleConstants.setForeground(this, bg)
                StyleConstants.setBackground(this, fg)
            }
            if (italic) StyleConstants.setItalic(this, true)
            if (bold) StyleConstants.setBold(this, true)
            if (strikethrough) StyleConstants.setStrikeThrough(this, true)
            if (underline) StyleConstants.setUnderline(this, true)
            // Don't support undercurl, underdouble, underdotted, and underdashed
            // Fallback to underline
            if (undercurl) StyleConstants.setUnderline(this, true)
            if (underdouble) StyleConstants.setUnderline(this, true)
            if (underdotted) StyleConstants.setUnderline(this, true)
            if (underdashed) StyleConstants.setUnderline(this, true)
        }
}
