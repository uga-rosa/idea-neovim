package com.ugarosa.neovim.mode

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.rd.util.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.atomic.AtomicReference

fun getMode() = service<NeovimModeManager>().get()

fun getAndSetMode(newMode: NeovimMode) = service<NeovimModeManager>().getAndSet(newMode)

private typealias OnModeChange = suspend (old: NeovimMode, new: NeovimMode) -> Unit

@Service(Service.Level.APP)
class NeovimModeManager(
    scope: CoroutineScope,
) {
    private val atomicMode = AtomicReference(NeovimMode.default)
    private val hooks = CopyOnWriteArrayList<OnModeChange>()

    @OptIn(ObsoleteCoroutinesApi::class)
    private val actor =
        scope.actor<Pair<NeovimMode, NeovimMode>>(capacity = Channel.UNLIMITED) {
            for ((oldMode, newMode) in channel) {
                for (hook in hooks) {
                    hook(oldMode, newMode)
                }
            }
        }

    fun get(): NeovimMode {
        return atomicMode.get()
    }

    fun getAndSet(newMode: NeovimMode): NeovimMode {
        val oldMode = atomicMode.getAndSet(newMode)
        if (oldMode == newMode) return oldMode

        actor.trySend(oldMode to newMode)
        return oldMode
    }

    fun addHook(hook: OnModeChange) {
        hooks.add(hook)
    }
}
