package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.util.concurrent.atomic.AtomicReference

fun getMode() = service<NeovimModeManager>().get()

fun setMode(newMode: NeovimMode) = service<NeovimModeManager>().set(newMode)

@Service(Service.Level.APP)
class NeovimModeManager {
    private val atomicMode = AtomicReference(NeovimMode.default)

    fun get(): NeovimMode {
        return atomicMode.get()
    }

    fun set(newMode: NeovimMode) {
        atomicMode.set(newMode)
    }
}
