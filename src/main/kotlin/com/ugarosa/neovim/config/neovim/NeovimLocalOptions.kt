package com.ugarosa.neovim.config.neovim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.config.neovim.option.Filetype
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
import com.ugarosa.neovim.rpc.BufferId
import com.ugarosa.neovim.rpc.client.NeovimRpcClientImpl
import com.ugarosa.neovim.rpc.function.getLocalOptions
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

class NeovimLocalOptionsManager private constructor() {
    companion object {
        private val logger = thisLogger()

        suspend fun create(bufferId: BufferId): NeovimLocalOptionsManager {
            val client = ApplicationManager.getApplication().service<NeovimRpcClientImpl>()
            val options =
                getLocalOptions(client, bufferId).getOrNull()
                    ?: let {
                        logger.warn("Failed to get local options for buffer $bufferId")
                        mapOf()
                    }
            return NeovimLocalOptionsManager().apply {
                putAll(options)
            }
        }
    }

    private val logger = thisLogger()
    private val mutex = Mutex()
    private val options = MutableNeovimLocalOptions()
    private val setters: Map<String, (Any) -> Unit> =
        mapOf(
            "filetype" to { raw -> options.filetype = Filetype.fromRaw(raw) },
            "scrolloff" to { raw -> options.scrolloff = Scrolloff.fromRaw(raw) },
            "sidescrolloff" to { raw -> options.sidescrolloff = Sidescrolloff.fromRaw(raw) },
        )

    suspend fun get(): NeovimLocalOptions = mutex.withLock { options.copy() }

    suspend fun put(
        name: String,
        raw: Any,
    ) {
        mutex.withLock {
            setters[name]?.invoke(raw)
                ?: logger.warn("Invalid option name: $name")
        }
    }

    suspend fun putAll(options: Map<String, Any>) {
        options.forEach { (name, raw) -> put(name, raw) }
    }
}
