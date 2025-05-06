package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import com.ugarosa.neovim.rpc.event.NeovimMode
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class NeovimModeManager {
    private val mode = AtomicReference(NeovimMode.default)

    fun getMode(): NeovimMode {
        return mode.get()
    }

    // Returns true if the value was different and was updated
    fun setMode(newMode: NeovimMode): Boolean {
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
