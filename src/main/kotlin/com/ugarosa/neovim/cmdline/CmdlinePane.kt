package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.ugarosa.neovim.rpc.event.redraw.CmdlineEvent
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.text.DefaultCaret
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.StyledEditorKit

class CmdlinePane : JTextPane() {
    init {
        font =
            runReadAction {
                val scheme = EditorColorsManager.getInstance().globalScheme
                Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
            }

        isEditable = false
        isFocusable = false
        editorKit = StyledEditorKit()
        isOpaque = true
        background = JBColor.PanelBackground

        // Show caret
        (caret as? DefaultCaret)?.apply {
            blinkRate = 0
            isVisible = true
            updatePolicy = DefaultCaret.ALWAYS_UPDATE
        }
    }

    private val logger = thisLogger()

    private var mainLine: List<CmdlineEvent.ShowChunk> = emptyList()
    private var cursorPos: Int = 0
    private var firstChar: String = ""
    private var indent: Int = 0
    private var specialChar: String = ""
    private var blockLines: MutableList<List<CmdlineEvent.ShowChunk>> = mutableListOf()

    fun updateModel(
        show: CmdlineEvent.Show? = null,
        pos: Int? = null,
        specialChar: String? = null,
        blockShow: List<List<CmdlineEvent.ShowChunk>>? = null,
        blockAppend: List<CmdlineEvent.ShowChunk>? = null,
    ) {
        show?.let {
            mainLine = it.content
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
            blockLines.add(it)
        }
    }

    fun clearSingle() {
        mainLine = emptyList()
        cursorPos = 0
        firstChar = ""
        indent = 0
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
        return mainLine.isEmpty() && blockLines.isEmpty()
    }

    fun flush() {
        logger.debug("Flushing CmdlinePane: mainLine=$mainLine, blockLines=$blockLines")

        val doc = styledDocument
        // Remove all text
        doc.remove(0, doc.length)

        var offset = 0
        blockLines.forEach { line ->
            offset = doc.insertLine(offset, line, 0, "", true)
        }

        val lineStartOffset = offset
        doc.insertLine(offset, mainLine, indent, specialChar, false)

        val cursorOffset = lineStartOffset + firstChar.length + indent + cursorPos
        caretPosition = cursorOffset
        requestFocusInWindow()
    }

    private fun StyledDocument.insertLine(
        startOffset: Int,
        line: List<CmdlineEvent.ShowChunk>,
        indent: Int,
        specialChar: String,
        newLine: Boolean,
    ): Int {
        var offset = startOffset

        if (firstChar.isNotEmpty()) {
            insertString(offset, firstChar, null)
            offset += firstChar.length
        }

        if (indent > 0) {
            insertString(offset, " ".repeat(indent), null)
            offset += indent
        }

        for (chunk in line) {
            val style = chunk.highlightAttributes.toAttributeSet()
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
