package com.ugarosa.neovim.config.neovim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Selection
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.config.neovim.option.getOrElse
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClientImpl
import com.ugarosa.neovim.rpc.event.OptionScope
import com.ugarosa.neovim.rpc.event.maybeOptionSetEvent
import com.ugarosa.neovim.rpc.function.getGlobalOptions
import com.ugarosa.neovim.rpc.function.hookGlobalOptionSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class NeovimOption(
    val filetype: Filetype,
    val selection: Selection,
    val scrolloff: Scrolloff,
    val sidescrolloff: Sidescrolloff,
)

@Service(Service.Level.APP)
class NeovimOptionManager(
    private val scope: CoroutineScope,
) {
    private val logger = thisLogger()
    private val client = ApplicationManager.getApplication().service<NeovimRpcClientImpl>()
    private val globalOptionsManager = NeovimGlobalOptionsManager()
    private val localOptionsManagers = ConcurrentHashMap<BufferId, Deferred<NeovimLocalOptionsManager>>()

    init {
        client.registerPushHandler { push ->
            maybeOptionSetEvent(push)?.let { event ->
                when (event.scope) {
                    OptionScope.LOCAL -> putLocal(event.bufferId, event.name, event.value)
                    OptionScope.GLOBAL -> putGlobal(event.name, event.value)
                }
            }
        }

        scope.launch {
            val globalOptions =
                getGlobalOptions(client).getOrNull()
                    ?: run {
                        logger.warn("Failed to get global options")
                        return@launch
                    }
            globalOptionsManager.putAll(globalOptions)

            hookGlobalOptionSet(client).onLeft {
                logger.warn("Failed to hook global option set: $it")
            }
        }
    }

    suspend fun getGlobal(): NeovimGlobalOptions {
        return globalOptionsManager.get()
    }

    suspend fun getLocal(bufferId: BufferId): NeovimOption {
        val globalOptions = globalOptionsManager.get()

        val localOptionsManager =
            localOptionsManagers.computeIfAbsent(bufferId) {
                scope.async { NeovimLocalOptionsManager.create(bufferId) }
            }.await()
        val localOptions = localOptionsManager.get()

        return NeovimOption(
            filetype = localOptions.filetype,
            selection = globalOptions.selection,
            scrolloff = localOptions.scrolloff.getOrElse(globalOptions.scrolloff),
            sidescrolloff = localOptions.sidescrolloff.getOrElse(globalOptions.sidescrolloff),
        )
    }

    suspend fun putGlobal(
        name: String,
        raw: Any,
    ) {
        logger.trace("Set a global option: $name = $raw")
        globalOptionsManager.put(name, raw)
    }

    suspend fun putLocal(
        bufferId: BufferId,
        name: String,
        raw: Any,
    ) {
        logger.trace("Set a local option: $name = $raw")
        val localOptionsManager =
            localOptionsManagers.computeIfAbsent(bufferId) {
                scope.async { NeovimLocalOptionsManager.create(bufferId) }
            }.await()
        localOptionsManager.put(name, raw)
    }
}
