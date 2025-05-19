package com.ugarosa.neovim.common

import com.intellij.openapi.util.ScalableIcon
import com.intellij.util.IconUtil
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * An Icon that draws [base] and then draws [badge] at one of the four corners,
 * with [padding] pixels inset.
 */
class OverlayIcon(
    private val base: Icon,
    private val badge: Icon,
    private val corner: Corner = Corner.TOP_RIGHT,
    private val padding: Int = 0,
) : ScalableIcon {
    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    override fun getIconWidth(): Int = base.iconWidth

    override fun getIconHeight(): Int = base.iconHeight

    override fun paintIcon(
        c: Component?,
        g: Graphics,
        x: Int,
        y: Int,
    ) {
        // draw the base icon
        base.paintIcon(c, g, x, y)

        // decide where to draw the badge
        val bx =
            when (corner) {
                Corner.TOP_LEFT, Corner.BOTTOM_LEFT -> x + padding
                Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT -> x + base.iconWidth - badge.iconWidth - padding
            }
        val by =
            when (corner) {
                Corner.TOP_LEFT, Corner.TOP_RIGHT -> y + padding
                Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT -> y + base.iconHeight - badge.iconHeight - padding
            }

        // draw the badge
        badge.paintIcon(c, g, bx, by)
    }

    override fun getScale(): Float = (base as? ScalableIcon)?.scale ?: 1.0f

    override fun scale(scale: Float): Icon {
        val scaledBase = (base as? ScalableIcon)?.scale(scale) ?: IconUtil.scale(base, null, scale)
        val scaledBadge = (badge as? ScalableIcon)?.scale(scale) ?: IconUtil.scale(badge, null, scale)
        return OverlayIcon(scaledBase, scaledBadge, corner, padding)
    }
}
