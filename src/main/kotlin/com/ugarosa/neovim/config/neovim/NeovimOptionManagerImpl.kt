package com.ugarosa.neovim.config.neovim

import com.intellij.openapi.components.Service
import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.getOrElse
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.BufferId
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class NeovimOptionManagerImpl : NeovimOptionManager {
    private val logger = myLogger()

    private val globalOptionsManager = NeovimGlobalOptionsManager()
    private val globalInit = CompletableDeferred<Unit>()

    private val localOptionsManagers = ConcurrentHashMap<BufferId, NeovimLocalOptionsManager>()
    private val localInits = ConcurrentHashMap<BufferId, CompletableDeferred<Unit>>()

    override suspend fun initializeGlobal() {
        globalOptionsManager.initialize()
        globalInit.complete(Unit)
    }

    override suspend fun initializeLocal(bufferId: BufferId) {
        val initDeferred = CompletableDeferred<Unit>()
        localInits[bufferId] = initDeferred
        val localOptionsManager = NeovimLocalOptionsManager().apply { initialize(bufferId) }
        localOptionsManagers[bufferId] = localOptionsManager
        initDeferred.complete(Unit)
    }

    override suspend fun getGlobal(): NeovimGlobalOptions {
        globalInit.await()
        return globalOptionsManager.get()
    }

    override suspend fun getLocal(bufferId: BufferId): NeovimOption {
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

    override suspend fun putGlobal(
        key: String,
        value: Any,
    ) {
        globalInit.await()
        logger.trace("Set a global option: $key = $value")
        globalOptionsManager.putAll(mapOf(key to value))
    }

    override suspend fun putLocal(
        bufferId: BufferId,
        key: String,
        value: Any,
    ) {
        localInits[bufferId]?.await()
            ?: throw IllegalStateException("Buffer $bufferId is not initialized")
        logger.trace("Set a local option: $key = $value")
        localOptionsManagers[bufferId]?.putAll(mapOf(key to value))
    }
}
