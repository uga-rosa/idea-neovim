package com.ugarosa.neovim.config.nvim

import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.nvim.option.Filetype
import com.ugarosa.neovim.config.nvim.option.Scrolloff
import com.ugarosa.neovim.config.nvim.option.Sidescrolloff
import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.getLocalOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface NvimLocalOptions {
    val filetype: Filetype
    val scrolloff: Scrolloff?
    val sidescrolloff: Sidescrolloff?
}

private data class MutableNvimLocalOptions(
    override var filetype: Filetype = Filetype.default,
    override var scrolloff: Scrolloff? = null,
    override var sidescrolloff: Sidescrolloff? = null,
) : NvimLocalOptions

class NvimLocalOptionsManager() {
    private val logger = myLogger()
    private val client = service<NvimClient>()
    private val mutex = Mutex()
    private val options = MutableNvimLocalOptions()
    private val setters: Map<String, (Any) -> Unit> =
        mapOf(
            "filetype" to { raw -> options.filetype = Filetype.fromRaw(raw) },
            "scrolloff" to { raw -> options.scrolloff = Scrolloff.fromRaw(raw) },
            "sidescrolloff" to { raw -> options.sidescrolloff = Sidescrolloff.fromRaw(raw) },
        )

    suspend fun initialize(bufferId: BufferId) {
        logger.trace("Initializing local options for buffer: $bufferId")
        val localOptions = client.getLocalOption(bufferId)
        putAll(localOptions)
    }

    suspend fun get(): NvimLocalOptions = mutex.withLock { options.copy() }

    suspend fun putAll(options: Map<String, Any>) {
        mutex.withLock {
            options.forEach { (name, raw) ->
                setters[name]?.invoke(raw)
                    ?: logger.warn("Invalid option name: $name")
            }
        }
    }
}
