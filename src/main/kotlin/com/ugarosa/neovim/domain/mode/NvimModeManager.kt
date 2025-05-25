package com.ugarosa.neovim.domain.mode

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.rd.util.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.atomic.AtomicReference

fun getMode() = service<NeovimModeManager>().get()

fun setMode(mode: NvimMode) = service<NeovimModeManager>().set(mode)

private typealias OnModeChange = suspend (old: NvimMode, new: NvimMode) -> Unit

@Service(Service.Level.APP)
class NeovimModeManager(
    scope: CoroutineScope,
) {
    private val atomicMode = AtomicReference(NvimMode.default)
    private val hooks = CopyOnWriteArrayList<OnModeChange>()

    @OptIn(ObsoleteCoroutinesApi::class)
    private val actor =
        scope.actor<Pair<NvimMode, NvimMode>>(capacity = Channel.UNLIMITED) {
            for ((oldMode, newMode) in channel) {
                for (hook in hooks) {
                    hook(oldMode, newMode)
                }
            }
        }

    fun get(): NvimMode {
        return atomicMode.get()
    }

    fun set(mode: NvimMode) {
        val oldMode = atomicMode.getAndSet(mode)
        actor.trySend(oldMode to mode)
    }

    fun addHook(hook: OnModeChange) {
        hooks.add(hook)
    }
}
