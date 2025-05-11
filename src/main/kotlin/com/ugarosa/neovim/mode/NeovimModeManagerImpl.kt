package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimModeManagerImpl : NeovimModeManager {
    private val atomicMode = AtomicReference(NeovimMode.default)

    override fun get(): NeovimMode {
        return atomicMode.get()
    }

    override fun set(newMode: NeovimMode) {
        atomicMode.set(newMode)
    }
}
