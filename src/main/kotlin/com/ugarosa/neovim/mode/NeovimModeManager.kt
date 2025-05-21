package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.ugarosa.neovim.common.focusEditor
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.statusline.StatusLineManager
import com.ugarosa.neovim.undo.NeovimUndoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun getMode() = service<NeovimModeManager>().get()

fun getAndSetMode(newMode: NeovimMode) = service<NeovimModeManager>().getAndSet(newMode)

@Service(Service.Level.APP)
class NeovimModeManager(
    private val scope: CoroutineScope,
) {
    private val atomicMode = AtomicReference(NeovimMode.default)

    fun get(): NeovimMode {
        return atomicMode.get()
    }

    fun getAndSet(newMode: NeovimMode): NeovimMode {
        val oldMode = atomicMode.getAndSet(newMode)
        if (oldMode == newMode) return oldMode

        scope.launch {
            val project = focusProject() ?: return@launch
            project.service<StatusLineManager>().updateStatusLine(newMode)
            if (newMode.isInsert()) {
                val editor = focusEditor() ?: return@launch
                service<NeovimUndoManager>().start(project, editor.document)
            } else if (oldMode.isInsert()) {
                service<NeovimUndoManager>().finish()
            }
        }
        return oldMode
    }
}
