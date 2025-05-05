package com.ugarosa.neovim.rpc.function

import arrow.core.Either
import arrow.core.raise.either
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
    private val inFlight = AtomicReference<CompletableDeferred<Either<NeovimFunctionError, Int>>?>(null)

    suspend fun fetch(client: NeovimRpcClient): Either<NeovimFunctionError, Int> =
        either {
            // Return cached value if available.
            cache.get()?.let { return@either it }

            // If not, check if a request is already in flight.
            val newDeferred = CompletableDeferred<Either<NeovimFunctionError, Int>>()
            val old = inFlight.getAndUpdate { current -> current ?: newDeferred }
            val deferred = old ?: newDeferred

            // If this is the first request, I need to make it.
            if (old == null) {
                newDeferred.complete(request(client))
            }

            // Wait for the result
            val outcome = deferred.await()
            // Set the result in the cache if successful.
            // Otherwise, clear the in-flight reference to allow retry.
            outcome.onRight { cache.set(it) }
                .onLeft { inFlight.set(null) }
                .bind()
        }

    private suspend fun request(client: NeovimRpcClient): Either<NeovimFunctionError, Int> =
        either {
            val result =
                client.request("nvim_get_chan_info", listOf(0))
                    .translate().bind()
            Either.catch {
                result.asMapValue().get("id")!!.asIntegerValue().toInt()
            }.mapLeft { NeovimFunctionError.ResponseTypeMismatch }.bind()
        }

    private fun MapValue.get(key: String): Value? =
        this.map().entries
            .firstOrNull { it.key.isStringValue && it.key.asStringValue().asString() == key }
            ?.value
}
