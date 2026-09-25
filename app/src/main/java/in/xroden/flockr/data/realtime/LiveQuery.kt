/** Keeps a query's result current by re-running it whenever the tables it reads change. */
package `in`.xroden.flockr.data.realtime

import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.network.RetryPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge

/** A table to watch, narrowed to rows whose [column] equals [value] when both are given. */
data class TableWatch(val table: String, val column: String? = null, val value: String? = null)

private val retryPolicy = RetryPolicy()

/**
 * Emits [fetch]'s result once the channel is subscribed, then again after each burst of changes to
 * [watches]. Row-level security applies to the change feed, so a member only hears about rows they
 * can read.
 *
 * Each collection opens its own channel: two collectors sharing one would let the first to cancel
 * tear down the channel the other is still using. A failure is emitted once and ends the flow, so a
 * retry is a fresh collection.
 */
@OptIn(FlowPreview::class)
fun <T> SupabaseClient.liveQuery(
    connectionManager: RealtimeConnectionManager,
    watches: List<TableWatch>,
    fetch: suspend () -> T
): Flow<Result<T>> = callbackFlow {
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
        channel.subscribe(blockUntilSubscribed = true)
        send(retryPolicy.execute { fetch() })
        merge(*changes.toTypedArray())
            .debounce(AppConstants.REALTIME_DEBOUNCE_MS)
            .collect { send(retryPolicy.execute { fetch() }) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        send(Result.failure(e))
        close()
    }
    awaitClose { connectionManager.removeChannelAsync(channel) }
}
