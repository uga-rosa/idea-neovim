package com.ugarosa.neovim.config.neovim

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.common.tryComplete
import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Selection
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.config.neovim.option.getOrElse
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.event.OptionScope
import com.ugarosa.neovim.rpc.event.maybeOptionSetEvent
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

data class NeovimOption(
    val filetype: Filetype,
    val selection: Selection,
    val scrolloff: Scrolloff,
    val sidescrolloff: Sidescrolloff,
)

@Service(Service.Level.APP)
class NeovimOptionManager {
    private val logger = thisLogger()
    private val client = getClient()

    private val globalOptionsManager = NeovimGlobalOptionsManager()
    private val globalInit = CompletableDeferred<Unit>()

    private val localOptionsManagers = ConcurrentHashMap<BufferId, NeovimLocalOptionsManager>()
    private val localInits = ConcurrentHashMap<BufferId, CompletableDeferred<Unit>>()

    init {
        client.registerPushHandler { push ->
            maybeOptionSetEvent(push)?.let { event ->
                logger.trace("Received an option set event: $event")
                when (event.scope) {
                    OptionScope.LOCAL -> putLocal(event.bufferId, event.name, event.value)
                    OptionScope.GLOBAL -> putGlobal(event.name, event.value)
                }
            }
        }
    }

    suspend fun initializeGlobal() {
        globalOptionsManager.initialize()
        globalInit.tryComplete(Unit)
    }

    suspend fun initializeLocal(bufferId: BufferId) {
        val initDeferred =
            localInits.computeIfAbsent(bufferId) {
                CompletableDeferred()
            }
        localOptionsManagers
            .computeIfAbsent(bufferId) { NeovimLocalOptionsManager() }
            .initialize(bufferId)
        initDeferred.tryComplete(Unit)
    }

    suspend fun getGlobal(): NeovimGlobalOptions {
        globalInit.await()
        return globalOptionsManager.get()
    }

    suspend fun getLocal(bufferId: BufferId): NeovimOption {
        globalInit.await()
        localInits[bufferId]?.await()
            ?: throw IllegalStateException("Buffer $bufferId is not initialized")

        val globalOptions = globalOptionsManager.get()
        val localOptions = localOptionsManagers[bufferId]?.get()

        return NeovimOption(
            filetype = localOptions?.filetype ?: Filetype.default,
            selection = globalOptions.selection,
            scrolloff = localOptions?.scrolloff.getOrElse(globalOptions.scrolloff),
            sidescrolloff = localOptions?.sidescrolloff.getOrElse(globalOptions.sidescrolloff),
        )
    }

    suspend fun putGlobal(
        name: String,
        raw: Any,
    ) {
        globalInit.await()
        logger.trace("Set a global option: $name = $raw")
        globalOptionsManager.putAll(mapOf(name to raw))
    }

    suspend fun putLocal(
        bufferId: BufferId,
        name: String,
        raw: Any,
    ) {
        localInits[bufferId]?.await()
            ?: throw IllegalStateException("Buffer $bufferId is not initialized")
        logger.trace("Set a local option: $name = $raw")
        localOptionsManagers[bufferId]?.putAll(mapOf(name to raw))
    }
}
