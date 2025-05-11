package com.ugarosa.neovim.cmdline

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

class CmdlinePanel : JPanel() {
    private var textChunks: List<CmdlineEvent.ShowContent> = emptyList()
    private var cursorPos: Int = 0
    private var specialChar: String? = null
    private var blockLines: MutableList<List<CmdlineEvent.ShowContent>>? = null

    init {
        isOpaque = true
        background = JBColor.PanelBackground
        border = JBUI.Borders.empty()
    }

    fun showCmdline(show: CmdlineEvent.Show) {
        textChunks = show.content
        cursorPos = show.pos
        specialChar = null
        revalidate()
        repaint()
    }

    fun updateCursor(pos: Int) {
        cursorPos = pos
        repaint()
    }

    fun showSpecialChar(
        c: String,
        shift: Boolean,
    ) {
        specialChar = c
        repaint()
    }

    fun showBlock(lines: List<List<CmdlineEvent.ShowContent>>) {
        blockLines = lines.toMutableList()
        textChunks = emptyList()
        specialChar = null
        revalidate()
        repaint()
    }

    fun appendBlockLine(line: List<CmdlineEvent.ShowContent>) {
        if (blockLines == null) blockLines = mutableListOf()
        blockLines!!.add(line)
        revalidate()
        repaint()
    }

    fun hideBlock() {
        blockLines = null
        clear()
    }

    fun clear() {
        textChunks = emptyList()
        specialChar = null
        cursorPos = 0
        revalidate()
        repaint()
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font)
        return if (blockLines != null) {
            val widths =
                blockLines!!.map { line ->
                    line.sumOf { it.text.length * fm.charWidth('W') } + JBUI.scale(20)
                }
            val maxWidth = widths.maxOrNull() ?: JBUI.scale(100)
            val height = blockLines!!.size * fm.height + JBUI.scale(8)
            Dimension(maxWidth, height)
        } else {
            val width = textChunks.sumOf { it.text.length * fm.charWidth('W') } + JBUI.scale(20)
            Dimension(width, fm.height + JBUI.scale(8))
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        val fm = g2.fontMetrics
        var y = JBUI.scale(4) + fm.ascent

        blockLines?.let { lines ->
            lines.forEach { line ->
                val x = JBUI.scale(10)
                drawSegments(g2, fm, line, x, y, null)
                y += fm.height
            }
            return
        }

        var x = JBUI.scale(10)
        x = drawSegments(g2, fm, textChunks, x, y, cursorPos)
        specialChar?.let {
            g2.color = JBColor.GREEN
            g2.drawString(it, x, y)
        }
    }

    private fun drawSegments(
        g2: Graphics2D,
        fm: FontMetrics,
        segments: List<CmdlineEvent.ShowContent>,
        startX: Int,
        y: Int,
        cursorIndex: Int?,
    ): Int {
        var x = startX
        var currentIndex = 0
        for ((attrs, text) in segments) {
            val ta = attrs.toTextAttributes()
            g2.font = g2.font.deriveFont(ta.fontType)
            g2.color = ta.foregroundColor ?: JBColor.foreground()
            g2.drawString(text, x, y)

            ta.effectType?.let { effect ->
                drawEffect(g2, fm, effect, ta.effectColor, x, y, text)
            }

            cursorIndex?.takeIf { it in currentIndex until currentIndex + text.length }?.let {
                val offsetX = fm.stringWidth(text.substring(0, it - currentIndex))
                val cursorX = x + offsetX
                g2.color = JBColor.WHITE
                g2.drawLine(cursorX, y + JBUI.scale(3), cursorX, y - font.size)
            }

            x += fm.stringWidth(text)
            currentIndex += text.length
        }
        return x
    }

    private fun drawEffect(
        g2: Graphics2D,
        fm: FontMetrics,
        effect: EffectType,
        effectColor: Color?,
        startX: Int,
        y: Int,
        text: String,
    ) {
        g2.color = effectColor ?: JBColor.GRAY
        val underlineY = y + JBUI.scale(2)
        val strikeY = y - JBUI.scale(font.size / 2)
        val width = fm.stringWidth(text)
        when (effect) {
            EffectType.LINE_UNDERSCORE -> g2.drawLine(startX, underlineY, startX + width, underlineY)
            EffectType.STRIKEOUT -> g2.drawLine(startX, strikeY, startX + width, strikeY)
            EffectType.WAVE_UNDERSCORE -> drawWaveLine(g2, startX, underlineY, width)
            else -> {}
        }
    }

    private fun drawWaveLine(
        g: Graphics2D,
        startX: Int,
        y: Int,
        width: Int,
    ) {
        val amplitude = 2
        val wavelength = 4
        var x = startX
        while (x < startX + width) {
            g.drawLine(x, y, x + 1, y + amplitude)
            g.drawLine(x + 1, y + amplitude, x + 2, y)
            x += wavelength
        }
    }
}
