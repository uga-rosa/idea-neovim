package com.ugarosa.neovim.config.nvim

import com.intellij.openapi.components.service
import com.ugarosa.neovim.config.nvim.option.Scrolloff
import com.ugarosa.neovim.config.nvim.option.Selection
import com.ugarosa.neovim.config.nvim.option.Sidescrolloff
import com.ugarosa.neovim.logger.myLogger
import com.ugarosa.neovim.rpc.client.NvimClient
import com.ugarosa.neovim.rpc.client.api.getGlobalOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface NeovimGlobalOptions {
    val selection: Selection
    val scrolloff: Scrolloff
    val sidescrolloff: Sidescrolloff
}

private data class MutableNeovimGlobalOptions(
    override var selection: Selection = Selection.default,
    override var scrolloff: Scrolloff = Scrolloff.default,
    override var sidescrolloff: Sidescrolloff = Sidescrolloff.default,
) : NeovimGlobalOptions

class NeovimGlobalOptionsManager() {
    private val logger = myLogger()
    private val client = service<NvimClient>()
    private val mutex = Mutex()
    private val options = MutableNeovimGlobalOptions()
    private val setters: Map<String, (Any) -> Unit> =
        mapOf(
            "selection" to { raw -> options.selection = Selection.fromRaw(raw) },
            "scrolloff" to { raw -> options.scrolloff = Scrolloff.fromRaw(raw) },
            "sidescrolloff" to { raw -> options.sidescrolloff = Sidescrolloff.fromRaw(raw) },
        )

    suspend fun initialize() {
        logger.trace("Initializing global options")
        val globalOptions = client.getGlobalOption()
        putAll(globalOptions)
    }

    suspend fun get(): NeovimGlobalOptions = mutex.withLock { options.copy() }

    suspend fun putAll(options: Map<String, Any>) {
        mutex.withLock {
            options.forEach { (name, raw) ->
                setters[name]?.invoke(raw)
                    ?: logger.info("Unknown option name: $name")
            }
        }
    }
}
