package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.ugarosa.neovim.common.focusProject
import com.ugarosa.neovim.statusline.StatusLineManager
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
        scope.launch {
            focusProject()?.service<StatusLineManager>()?.updateStatusLine(newMode)
        }
        return atomicMode.getAndSet(newMode)
    }
}
