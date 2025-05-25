package com.ugarosa.neovim.rpc.client

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.ugarosa.neovim.rpc.connection.NvimConnectionManager
import com.ugarosa.neovim.rpc.event.NeovimEventDispatcher
import com.ugarosa.neovim.rpc.event.NotificationHandler
import com.ugarosa.neovim.rpc.process.NvimProcessManager
import com.ugarosa.neovim.rpc.transport.NvimObject
import com.ugarosa.neovim.rpc.transport.NvimTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class NvimClient(
    val scope: CoroutineScope,
) : Disposable {
    private val processManager = NvimProcessManager()
    private val transport = NvimTransport(processManager)
    private val connectionManager = NvimConnectionManager(transport, scope)
    private val dispatcher = NeovimEventDispatcher(connectionManager, scope)

    internal val deferredChanId = CompletableDeferred<Long>()

    init {
        // health check
        scope.launch {
            val result = connectionManager.request("nvim_get_api_info", emptyList())
            val chanId = result.asArray()[0].asLong()
            deferredChanId.complete(chanId)
        }
    }

    fun register(
        method: String,
        handler: NotificationHandler,
    ) {
        dispatcher.register(method, handler)
    }

    @Suppress("unused")
    fun unregister(
        method: String,
        handler: NotificationHandler,
    ) {
        dispatcher.unregister(method, handler)
    }

    internal suspend fun request(
        method: String,
        args: List<Any> = emptyList(),
    ): NvimObject {
        deferredChanId.await()
        return connectionManager.request(method, args)
    }

    internal suspend fun notify(
        method: String,
        args: List<Any> = emptyList(),
    ) {
        deferredChanId.await()
        connectionManager.notify(method, args)
    }

    internal suspend fun execLua(
        packageName: String,
        method: String,
        args: List<Any> = emptyList(),
    ): NvimObject {
        val code = "return require('intellij.$packageName').$method(...)"
        return request("nvim_exec_lua", listOf(code, args))
    }

    internal suspend fun execLuaNotify(
        packageName: String,
        method: String,
        args: List<Any> = emptyList(),
    ) {
        val code = "require('intellij.$packageName').$method(...)"
        notify("nvim_exec_lua", listOf(code, args))
    }

    override fun dispose() {
        connectionManager.close()
        dispatcher.clear()
    }
}
