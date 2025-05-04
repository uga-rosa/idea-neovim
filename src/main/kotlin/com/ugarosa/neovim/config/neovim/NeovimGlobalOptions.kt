package com.ugarosa.neovim.config.neovim

import com.intellij.openapi.diagnostic.thisLogger
import com.ugarosa.neovim.config.neovim.option.Scrolloff
import com.ugarosa.neovim.config.neovim.option.Selection
import com.ugarosa.neovim.config.neovim.option.Sidescrolloff
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

class NeovimGlobalOptionsManager {
    private val logger = thisLogger()
    private val mutex = Mutex()
    private val options = MutableNeovimGlobalOptions()
    private val setters: Map<String, (Any) -> Unit> =
        mapOf(
            "selection" to { raw -> options.selection = Selection.fromRaw(raw) },
            "scrolloff" to { raw -> options.scrolloff = Scrolloff.fromRaw(raw) },
            "sidescrolloff" to { raw -> options.sidescrolloff = Sidescrolloff.fromRaw(raw) },
        )

    suspend fun get(): NeovimGlobalOptions = mutex.withLock { options.copy() }

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
