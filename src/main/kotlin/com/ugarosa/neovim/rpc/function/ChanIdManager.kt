package com.ugarosa.neovim.rpc.function

import com.ugarosa.neovim.rpc.client.NeovimRpcClient
import kotlinx.coroutines.CompletableDeferred
import org.msgpack.value.MapValue
import org.msgpack.value.Value
import java.util.concurrent.atomic.AtomicReference

/**
 * This class is responsible for managing the channel ID used for communication with Neovim.
 * It ensures that the channel ID is fetched only once and cached for future use.
 */
object ChanIdManager {
    private val cache = AtomicReference<Int?>(null)
    private val inFlight = AtomicReference<CompletableDeferred<Int>?>(null)

    suspend fun fetch(client: NeovimRpcClient): Int {
        // Return cached value if available.
        cache.get()?.let { return it }

        // If not, check if a request is already in flight.
        val newDeferred = CompletableDeferred<Int>()
        val old = inFlight.getAndUpdate { current -> current ?: newDeferred }
        val deferred = old ?: newDeferred

        // If this is the first request, I need to make it.
        if (old == null) {
            request(client)?.let { newDeferred.complete(it) }
                ?: run {
                    newDeferred.cancel()
                    inFlight.set(null)
                }
        }

        // Wait for the result
        return deferred.await().also { cache.set(it) }
    }

    private suspend fun request(client: NeovimRpcClient): Int? =
        client.request("nvim_get_chan_info", listOf(0))
            ?.decode { it.asMapValue().get("id")!!.asIntegerValue().toInt() }

    private fun MapValue.get(key: String): Value? =
        this.map().entries
            .firstOrNull { it.key.isStringValue && it.key.asStringValue().asString() == key }
            ?.value
}
