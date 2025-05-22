package com.ugarosa.neovim.undo

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.impl.DocumentUndoProvider
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoUtil
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.util.DocumentUtil
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.mode.NeovimModeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This is used to group changes made during a certain period into a single undo block.
 * It temporarily disables all other undo recordings, and only registers a custom undo action when `finish()` is called.
 *
 * NOTE: Why not group them using `executeCommand` with the same `groupId`?
 * There are many actions that modify the document besides `TypedAction`. For example, `Backspace` is a different
 * action, as are inserting completion suggestions or Copilot suggestions. There might also be plugins I’m unaware of
 * that provide their own actions. It’s not realistic to override all of these actions.
 *
 * NOTE: Why not use CommandProcessorEx.startCommand?
 * If the user types within a started command, processes like Lookup won’t be triggered, so the user won’t benefit from
 * auto-completion.
 */
@Service(Service.Level.APP)
class NeovimUndoManager {
    private val listener = DocumentUndoListener()
    private var beforeCaret = 0

    fun install() {
        service<NeovimModeManager>().addHook { old, new ->
            val editor = focusEditor() ?: return@addHook
            if (new.isInsert()) {
                start(editor)
            } else if (old.isInsert()) {
                val project = focusProject() ?: return@addHook
                finish(project, editor)
            }
        }
    }

    private suspend fun start(editor: EditorEx) =
        withContext(Dispatchers.EDT) {
            UndoUtil.disableUndoFor(editor.document)
            listener.clear()
            editor.document.addDocumentListener(listener)
            beforeCaret = editor.caretModel.offset
        }

    private suspend fun finish(
        project: Project,
        editor: EditorEx,
    ) = withContext(Dispatchers.EDT) {
        try {
            val patches = listener.copyPatches()
            val beforeCaret = this@NeovimUndoManager.beforeCaret
            val afterCaret = editor.caretModel.offset
            if (patches.isEmpty() && beforeCaret == afterCaret) {
                // No changes
                return@withContext
            }
            val reference = DocumentReferenceManager.getInstance().create(editor.document)

            val action =
                object : UndoableAction {
                    override fun undo() {
                        apply(editor, patches, beforeCaret, false)
                    }

                    override fun redo() {
                        apply(editor, patches, afterCaret, true)
                    }

                    override fun getAffectedDocuments(): Array<DocumentReference> = arrayOf(reference)

                    override fun isGlobal(): Boolean = false
                }

            runUndoTransparentWriteAction {
                UndoManager.getInstance(project).undoableActionPerformed(action)
            }
        } finally {
            UndoUtil.enableUndoFor(editor.document)
            editor.document.removeDocumentListener(listener)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun apply(
        editor: EditorEx,
        patches: List<Patch>,
        caretOffset: Int,
        forward: Boolean,
    ) {
        DocumentUndoProvider.startDocumentUndo(editor.document)
        try {
            DocumentUtil.writeInRunUndoTransparentAction {
                val seq = if (forward) patches else patches.reversed()
                seq.forEach { p ->
                    // replaceString(startOffset, endOffset, replacement)
                    //
                    // Important: `endOffset` **must be calculated using the text that is
                    // currently present in the document** at the moment of execution,
                    // because IntelliJ measures ranges in the *live* document.
                    //
                    // ─ forward == true  (redo)
                    //   The document still contains the *old* text, so the range to
                    //   replace is [offset, offset + old.length).
                    //
                    // ─ forward == false (undo)
                    //   The document currently contains the *new* text, so the range to
                    //   replace is [offset, offset + new.length).
                    editor.document.replaceString(
                        p.offset,
                        p.offset + if (forward) p.oldText.length else p.newText.length,
                        if (forward) p.newText else p.oldText,
                    )
                }
                editor.caretModel.moveToOffset(caretOffset)
            }
        } finally {
            DocumentUndoProvider.finishDocumentUndo(editor.document)
        }
    }
}
