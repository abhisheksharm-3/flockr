package `in`.xroden.flockr.data.base

import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.network.RetryPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.util.UUID

abstract class BaseRealtimeRepository(
    protected val supabase: SupabaseClient,
    private val connectionManager: RealtimeConnectionManager,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {

    /**
     * Creates a realtime flow with automatic retry on failures.
     * Uses unique channel IDs to prevent subscription conflicts.
     */
    @OptIn(FlowPreview::class)
    protected fun <T> createRealtimeFlow(
        channelId: String,
        table: String,
        filterColumn: String,
        filterValue: String,
        fetchData: suspend () -> Result<List<T>>
    ): Flow<Result<List<T>>> = callbackFlow {
        // Use unique channel ID to prevent reuse of already-subscribed channels
        val uniqueChannelId = "${channelId}_${UUID.randomUUID()}"
        val channel = supabase.realtime.channel(uniqueChannelId)

        try {
            // Set up change flow BEFORE subscribing (required by Supabase SDK)
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
                filter(FilterOperation(filterColumn, FilterOperator.EQ, filterValue))
            }

            // Subscribe to channel
            channel.subscribe(blockUntilSubscribed = true)

            // Fetch and send initial data AFTER subscription
            val initialResult = retryPolicy.execute {
                fetchData().getOrThrow()
            }
            send(initialResult)

            changeFlow
                .debounce(AppConstants.REALTIME_DEBOUNCE_MS)
                .collect {
                    val updatedResult = retryPolicy.execute {
                        fetchData().getOrThrow()
                    }
                    send(updatedResult)
                }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            launch { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }
}
