package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

class CmdlinePanel : JPanel() {
    private val logger = thisLogger()

    private var textChunks: List<CmdlineEvent.ShowChunk> = emptyList()
    private var cursorPos: Int = 0
    private var firstChar: String = ""
    private var indent: Int = 0
    private var specialChar: String? = null
    private var blockLines: MutableList<List<CmdlineEvent.ShowChunk>>? = null

    init {
        isOpaque = true
        background = JBColor.PanelBackground
        border = JBUI.Borders.empty()

        font =
            runReadAction {
                val scheme = EditorColorsManager.getInstance().globalScheme
                val fontName = scheme.editorFontName
                val fontSize = scheme.editorFontSize
                Font(fontName, Font.PLAIN, fontSize)
            }
    }

    fun updateModel(
        show: CmdlineEvent.Show? = null,
        pos: Int? = null,
        specialChar: String? = null,
        blockShow: List<List<CmdlineEvent.ShowChunk>>? = null,
        blockAppend: List<CmdlineEvent.ShowChunk>? = null,
    ) {
        show?.let {
            textChunks = it.content
            cursorPos = it.pos
            firstChar = it.firstChar
            indent = it.indent
        }
        pos?.let {
            cursorPos = it
        }
        specialChar?.let {
            this.specialChar = it
        }
        blockShow?.let {
            blockLines = it.toMutableList()
        }
        blockAppend?.let {
            blockLines = blockLines ?: mutableListOf()
            blockLines!!.add(it)
        }
    }

    fun clearSingle() {
        textChunks = emptyList()
        cursorPos = 0
        firstChar = ""
        indent = 0
        specialChar = null
    }

    fun clearBlock() {
        blockLines = null
    }

    fun isHidden(): Boolean {
        return textChunks.isEmpty() && blockLines == null
    }

    fun flush() {
        logger.debug("Flushing CmdlinePanel: $textChunks, $blockLines")
        revalidate()
        repaint()
    }

    private val widthOffset = JBUI.scale(20)
    private val heightOffset = JBUI.scale(8)

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font)
        val lineHeight = fm.height
        val charWidth = fm.charWidth('W')
        val prefixWidth = fm.stringWidth(firstChar)
        val indentWidth = indent * charWidth

        blockLines?.let { lines ->
            val blockWidths =
                lines.map { line ->
                    line.sumOf { it.text.length * charWidth }
                }
            val mainContentWidth = indentWidth + textChunks.sumOf { it.text.length * charWidth }
            val maxWidth = prefixWidth + (blockWidths + mainContentWidth).max()
            val totalLines = lines.size + 1
            val height = totalLines * lineHeight
            return Dimension(maxWidth + widthOffset, height + heightOffset)
        }

        val lineWidth = indentWidth + prefixWidth + textChunks.sumOf { it.text.length * charWidth }
        return Dimension(lineWidth + widthOffset, fm.height + heightOffset)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        // Apply anti-aliasing for better text rendering
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
        )
        g2.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON,
        )

        val fm = g2.fontMetrics
        val charWidth = fm.charWidth('W')
        var y = fm.ascent + heightOffset / 2

        fun writeFirstChar(x: Int): Int {
            if (firstChar.isEmpty()) return x
            g2.color = JBColor.foreground()
            g2.drawString(firstChar, x, y)
            return x + fm.stringWidth(firstChar)
        }

        val prefixX = widthOffset / 2
        blockLines?.let { lines ->
            lines.forEach { line ->
                val x = writeFirstChar(prefixX)
                drawSegments(g2, fm, line, x, y, null)
                y += fm.height
            }
        }

        var x = writeFirstChar(prefixX)
        x += indent * charWidth
        x = drawSegments(g2, fm, textChunks, x, y, cursorPos)
        specialChar?.let {
            g2.color = JBColor.GREEN
            g2.drawString(it, x, y)
        }
    }

    private fun drawSegments(
        g2: Graphics2D,
        fm: FontMetrics,
        segments: List<CmdlineEvent.ShowChunk>,
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
