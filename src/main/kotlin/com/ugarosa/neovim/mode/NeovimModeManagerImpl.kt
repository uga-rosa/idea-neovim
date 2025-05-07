package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimModeManagerImpl : NeovimModeManager {
    private val mode = AtomicReference(NeovimMode.default)

    override fun getMode(): NeovimMode {
        return mode.get()
    }

    // Returns true if the value was different and was updated
    override fun setMode(newMode: NeovimMode): Boolean {
        val old =
            mode.getAndUpdate { cur ->
                if (cur == newMode) {
                    cur
                } else {
                    newMode
                }
            }
        return old != newMode
    }
}
