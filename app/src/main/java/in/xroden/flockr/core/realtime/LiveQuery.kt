/** Keeps a query's result current by re-running it whenever the tables it reads change. */
package `in`.xroden.flockr.core.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A table to watch, narrowed to rows whose [column] equals [value] when both are given. */
data class TableWatch(val table: String, val column: String? = null, val value: String? = null)

private val CHANGE_BURST = 150.milliseconds
private val FIRST_RETRY_AFTER = 500.milliseconds
private const val ATTEMPTS = 3
private val SUBSCRIBE_WITHIN = 10.seconds
private val RECONNECT_AFTER = 5.seconds
private val RECONNECT_AT_MOST = 1.minutes

/** Closes channels after their collector is gone, which a cancelled collector's own scope can no longer do. */
private val channelCleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Emits [fetch]'s result once the channel is subscribed, then again after each burst of changes to
 * [watches]. Row-level security applies to the change feed, so a member only hears about rows they
 * can read.
 *
 * With [cached], the last good result is emitted first, from disk, and each fresh one replaces it.
 * Once any result has been shown, a lost connection is retried quietly with growing pauses instead of
 * failing, so the screen keeps what it has. A failure before anything was shown is emitted once and
 * ends the flow, so a retry is a fresh collection.
 *
 * Each collection opens its own channel: two collectors sharing one would let the first to cancel
 * tear down the channel the other is still using.
 */
@OptIn(FlowPreview::class)
fun <T> SupabaseClient.liveQuery(
    watches: List<TableWatch>,
    cached: CachedAs<T>? = null,
    fetch: suspend () -> T,
): Flow<Result<T>> = callbackFlow {
    var hasShown = false
    cached?.let { OfflineCache.read(it) }?.let {
        send(Result.success(it))
        hasShown = true
    }
    var pause = RECONNECT_AFTER
    while (true) {
        val channel = realtime.channel("live_${watches.first().table}_${UUID.randomUUID()}")
        try {
            val changes = watches.map { watch ->
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = watch.table
                    if (watch.column != null && watch.value != null) {
                        filter(FilterOperation(watch.column, FilterOperator.EQ, watch.value))
                    }
                }
            }
            withTimeoutOrNull(SUBSCRIBE_WITHIN) { channel.subscribe(blockUntilSubscribed = true) }
                ?: throw IOException("Couldn't reach Flockr")
            suspend fun refresh() {
                val value = withRetries(fetch).getOrThrow()
                send(Result.success(value))
                hasShown = true
                pause = RECONNECT_AFTER
                cached?.let { OfflineCache.write(it, value) }
            }
            refresh()
            merge(*changes.toTypedArray()).debounce(CHANGE_BURST).collect { refresh() }
        } catch (e: CancellationException) {
            closeChannel(channel)
            throw e
        } catch (e: Exception) {
            closeChannel(channel)
            if (!hasShown) {
                send(Result.failure(e))
                close()
                break
            }
            delay(pause)
            pause = (pause * 2).coerceAtMost(RECONNECT_AT_MOST)
        }
    }
    awaitClose()
}

private fun SupabaseClient.closeChannel(channel: RealtimeChannel) {
    channelCleanup.launch { runCatching { realtime.removeChannel(channel) } }
}

/** [fetch], tried up to three times with doubling pauses; a request the server refused (4xx) isn't retried. */
private suspend fun <T> withRetries(fetch: suspend () -> T): Result<T> {
    var pause = FIRST_RETRY_AFTER
    repeat(ATTEMPTS - 1) {
        val result = runCatching { fetch() }
        val error = result.exceptionOrNull() ?: return result
        if (error is CancellationException) throw error
        if (error is RestException && error.statusCode in 400..499) return result
        delay(pause)
        pause *= 2
    }
    return runCatching { fetch() }.onFailure { if (it is CancellationException) throw it }
}
