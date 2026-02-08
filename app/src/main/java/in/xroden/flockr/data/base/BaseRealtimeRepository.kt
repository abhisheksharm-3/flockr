package `in`.xroden.flockr.data.base

import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.network.RetryPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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

/** Base repository for Supabase realtime data operations. */
abstract class BaseRealtimeRepository(
    protected val supabase: SupabaseClient,
    private val connectionManager: RealtimeConnectionManager,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {
    protected val authenticatedUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    /** Creates a realtime flow with automatic retry on failures. */
    @OptIn(FlowPreview::class)
    protected fun <T> createRealtimeFlow(
        channelId: String,
        table: String,
        filterColumn: String,
        filterValue: String,
        fetchData: suspend () -> Result<List<T>>
    ): Flow<Result<List<T>>> = callbackFlow {
        val uniqueChannelId = "${channelId}_${UUID.randomUUID()}"
        val channel = supabase.realtime.channel(uniqueChannelId)

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
                filter(FilterOperation(filterColumn, FilterOperator.EQ, filterValue))
            }

            channel.subscribe(blockUntilSubscribed = true)

            val initialResult = retryPolicy.execute { fetchData().getOrThrow() }
            send(initialResult)

            changeFlow.debounce(AppConstants.REALTIME_DEBOUNCE_MS).collect {
                val updatedResult = retryPolicy.execute { fetchData().getOrThrow() }
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
