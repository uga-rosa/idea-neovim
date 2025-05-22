package com.ugarosa.neovim.undo

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

data class Patch(
    val offset: Int,
    val oldText: String,
    val newText: String,
)

class DocumentUndoListener : DocumentListener {
    private val patches = mutableListOf<Patch>()

    override fun documentChanged(event: DocumentEvent) {
        patches +=
            Patch(
                offset = event.offset,
                oldText = event.oldFragment.toString(),
                newText = event.newFragment.toString(),
            )
    }

    fun clear() {
        patches.clear()
    }

    fun copyPatches(): List<Patch> {
        return patches.toList()
    }
}
