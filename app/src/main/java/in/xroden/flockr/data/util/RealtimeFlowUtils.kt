package `in`.xroden.flockr.data.util

import `in`.xroden.flockr.core.constants.AppConstants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Creates a realtime Flow that subscribes to table changes and fetches fresh data.
 * Includes debouncing and proper channel cleanup.
 *
 * @param supabase The Supabase client
 * @param channelPrefix Prefix for the channel ID (e.g., "one_time_expenses")
 * @param table The database table to subscribe to
 * @param filterColumn Column to filter on (e.g., "house_id")
 * @param filterValue Value to filter by
 * @param fetchData Function to fetch fresh data from the database
 */
@OptIn(FlowPreview::class)
fun <T> createRealtimeFlow(
    supabase: SupabaseClient,
    channelPrefix: String,
    table: String,
    filterColumn: String,
    filterValue: String,
    fetchData: suspend () -> Result<List<T>>
): Flow<Result<List<T>>> = callbackFlow {
    val uniqueChannelId = "${channelPrefix}_${filterValue}_${UUID.randomUUID()}"
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
        send(fetchData())

        // Collect changes with debounce
        changeFlow
            .debounce(AppConstants.REALTIME_DEBOUNCE_MS)
            .collect {
                send(fetchData())
            }
    } catch (e: Exception) {
        send(Result.failure(e))
    }

    awaitClose {
        launch { runCatching { supabase.realtime.removeChannel(channel) } }
    }
}
