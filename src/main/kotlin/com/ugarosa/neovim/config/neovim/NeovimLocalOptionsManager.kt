package com.ugarosa.neovim.config.neovim

import arrow.core.getOrElse
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.common.getClient
import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.function.getLocalOptions
import com.ugarosa.neovim.rpc.function.hookLocalOptionSet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface NeovimLocalOptions {
    val filetype: Filetype
    val scrolloff: Scrolloff?
    val sidescrolloff: Sidescrolloff?
}

private data class MutableNeovimLocalOptions(
    override var filetype: Filetype = Filetype.default,
    override var scrolloff: Scrolloff? = null,
    override var sidescrolloff: Sidescrolloff? = null,
) : NeovimLocalOptions

class NeovimLocalOptionsManager() {
    private val logger = thisLogger()
    private val client = getClient()
    private val mutex = Mutex()
    private val options = MutableNeovimLocalOptions()
    private val setters: Map<String, (Any) -> Unit> =
        mapOf(
            "filetype" to { raw -> options.filetype = Filetype.fromRaw(raw) },
            "scrolloff" to { raw -> options.scrolloff = Scrolloff.fromRaw(raw) },
            "sidescrolloff" to { raw -> options.sidescrolloff = Sidescrolloff.fromRaw(raw) },
        )

    suspend fun initialize(bufferId: BufferId) {
        logger.trace("Initializing local options for buffer: $bufferId")
        val localOptions =
            getLocalOptions(client, bufferId).getOrElse {
                logger.warn("Failed to get local options for buffer $bufferId")
                mapOf()
            }
        putAll(localOptions)
        hookLocalOptionSet(client, bufferId).onLeft {
            logger.warn("Failed to hook local option set for buffer $bufferId: $it")
        }
    }

    suspend fun get(): NeovimLocalOptions = mutex.withLock { options.copy() }

    suspend fun putAll(options: Map<String, Any>) {
        mutex.withLock {
            options.forEach { (name, raw) ->
                setters[name]?.invoke(raw)
                    ?: logger.warn("Invalid option name: $name")
            }
        }
    }
}
