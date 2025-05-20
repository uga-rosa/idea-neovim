package com.ugarosa.neovim.cmdline

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.ugarosa.neovim.common.BaseEditorViewer
import com.ugarosa.neovim.logger.myLogger

@Service(Service.Level.PROJECT)
class CmdlineView(
    project: Project,
) : BaseEditorViewer(project) {
    private val logger = myLogger()

    private val emptyShow = CmdlineEvent.Show(emptyList(), 0, "", "", 0, 0, 0)
    private var show: CmdlineEvent.Show = emptyShow
    private var specialChar: String = ""
    private var blockLines: MutableList<List<CmdChunk>> = mutableListOf()
    private var isDirty = false

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
        isDirty = true
    }

    fun clearSingle() {
        show = emptyShow
        specialChar = ""
        isDirty = true
    }

    fun clearBlock() {
        blockLines = mutableListOf()
        isDirty = true
    }

    fun isHidden(): Boolean = show.level == 0

    fun flush(): Boolean {
        if (!isDirty) return false
        isDirty = false

        logger.trace("Flushing CmdlinePane: show=$show, specialChar=$specialChar, blockLines=$blockLines")

        runUndoTransparentWriteAction {
            // Clear all text and highlighters
            document.setText("")
            editor.markupModel.removeAllHighlighters()

            var offset = 0
            blockLines.forEach { line ->
                offset = document.insertLine(offset, line, 0, "", true)
            }

            val lineStartOffset = offset
            offset = document.insertLine(offset, show.content, show.indent, specialChar, false)

            val cursorOffset = lineStartOffset + show.firstChar.length + show.prompt.length + show.indent + show.pos
            editor.caretModel.moveToOffset(cursorOffset)

            drawFakeCaret()
        }
        return true
    }

    private fun Document.insertLine(
        startOffset: Int,
        line: List<CmdChunk>,
        indent: Int,
        specialChar: String,
        newLine: Boolean,
    ): Int {
        var offset = startOffset
        val markup = editor.markupModel

        if (show.firstChar.isNotEmpty()) {
            insertString(offset, show.firstChar)
            offset += show.firstChar.length
        }

        if (show.prompt.isNotEmpty()) {
            val length = show.prompt.length
            val attrs = highlightManager.get(show.hlId).toTextAttributes()
            insertString(offset, show.prompt)
            markup.addRangeHighlighter(
                offset,
                offset + length,
                HighlighterLayer.SYNTAX,
                attrs,
                HighlighterTargetArea.EXACT_RANGE,
            )
            offset += length
        }

        if (indent > 0) {
            insertString(offset, " ".repeat(indent))
            offset += indent
        }

        line.forEach { chunk ->
            val length = chunk.text.length
            val attrs = highlightManager.get(chunk.attrId).toTextAttributes()
            insertString(offset, chunk.text)
            markup.addRangeHighlighter(
                offset,
                offset + length,
                HighlighterLayer.SYNTAX,
                attrs,
                HighlighterTargetArea.EXACT_RANGE,
            )
            offset += length
        }

        if (specialChar.isNotEmpty()) {
            val length = specialChar.length
            val attrs = TextAttributes().apply { foregroundColor = JBColor.GREEN }
            insertString(offset, specialChar)
            markup.addRangeHighlighter(
                offset,
                offset + length,
                HighlighterLayer.SYNTAX,
                attrs,
                HighlighterTargetArea.EXACT_RANGE,
            )
            offset += specialChar.length
        }

        if (newLine) {
            insertString(offset, "\n")
            offset += 1
        }

        return offset
    }
}
