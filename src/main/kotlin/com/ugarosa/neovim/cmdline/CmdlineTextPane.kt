package com.ugarosa.neovim.cmdline

import com.intellij.ui.JBColor
import com.ugarosa.neovim.common.BaseTextPane
import javax.swing.text.DefaultCaret
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class CmdlineTextPane : BaseTextPane() {
    init {
        // Show caret
        (caret as? DefaultCaret)?.apply {
            blinkRate = 0
            isVisible = true
            updatePolicy = DefaultCaret.ALWAYS_UPDATE
        }
    }

    private val emptyShow = CmdlineEvent.Show(emptyList(), 0, "", "", 0, 0)
    private var show: CmdlineEvent.Show = emptyShow
    private var specialChar: String = ""
    private var blockLines: MutableList<List<CmdChunk>> = mutableListOf()

    fun updateModel(
        show: CmdlineEvent.Show? = null,
        pos: Int? = null,
        specialChar: String? = null,
        blockShow: List<List<CmdChunk>>? = null,
        blockAppend: List<CmdChunk>? = null,
    ) {
        show?.let {
            this.show = it
            // Should be hidden at next cmdline_show
            this.specialChar = ""
        }
        pos?.let {
            this.show = this.show.copy(pos = it)
        }
        specialChar?.let {
            this.specialChar = it
        }
        blockShow?.let {
            blockLines = it.toMutableList()
        }
        blockAppend?.let {
            blockLines.add(it)
        }
    }

    fun clearSingle() {
        show = emptyShow
        specialChar = ""
    }

    fun clearBlock() {
        blockLines = mutableListOf()
    }

    fun reset() {
        clearSingle()
        clearBlock()
        val doc = styledDocument
        doc.remove(0, doc.length)
    }

    fun isHidden(): Boolean {
        return show.level == 0
    }

    fun flush() {
        logger.trace("Flushing CmdlinePane: show=$show, specialChar=$specialChar, blockLines=$blockLines")

        val doc = styledDocument
        // Remove all text
        doc.remove(0, doc.length)

        var offset = 0
        blockLines.forEach { line ->
            offset = doc.insertLine(offset, line, 0, "", true)
        }

        val lineStartOffset = offset
        doc.insertLine(offset, show.content, show.indent, specialChar, false)

        val cursorOffset = lineStartOffset + show.firstChar.length + show.indent + show.pos
        caretPosition = cursorOffset
        requestFocusInWindow()
    }

    private fun StyledDocument.insertLine(
        startOffset: Int,
        line: List<CmdChunk>,
        indent: Int,
        specialChar: String,
        newLine: Boolean,
    ): Int {
        var offset = startOffset

        if (show.firstChar.isNotEmpty()) {
            insertString(offset, show.firstChar, null)
            offset += show.firstChar.length
        }

        if (indent > 0) {
            insertString(offset, " ".repeat(indent), null)
            offset += indent
        }

        for (chunk in line) {
            val style = highlightManager.get(chunk.attrId).toAttributeSet()
            insertString(offset, chunk.text, style)
            offset += chunk.text.length
        }

        if (specialChar.isNotEmpty()) {
            val style = addStyle(null, null)
            StyleConstants.setForeground(style, JBColor.GREEN)
            insertString(offset, specialChar, style)
            offset += specialChar.length
        }

        if (newLine) {
            insertString(offset, "\n", null)
            offset += 1
        }

        return offset
    }
}
