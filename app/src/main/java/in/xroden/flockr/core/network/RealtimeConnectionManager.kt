package `in`.xroden.flockr.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for Supabase realtime connections.
 * Handles channel lifecycle, connection health monitoring, and automatic reconnection.
 */
@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()

    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    init {
        monitorNetworkChanges()
    }

    /**
     * Gets or creates a realtime channel with the given ID.
     * Reuses existing channels to prevent duplicate subscriptions.
     */
    fun getOrCreateChannel(channelId: String): RealtimeChannel {
        return activeChannels.getOrPut(channelId) {
            supabase.realtime.channel(channelId)
        }
    }

    /**
     * Removes a channel and cleans up its subscription.
     */
    fun removeChannel(channelId: String) {
        activeChannels.remove(channelId)?.let { channel ->
            scope.launch {
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }
    }

    /**
     * Removes a channel by reference.
     */
    fun removeChannel(channel: RealtimeChannel) {
        val entry = activeChannels.entries.find { it.value == channel }
        entry?.let {
            activeChannels.remove(it.key)
            scope.launch {
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }
    }

    /**
     * Cleans up all active channels.
     * Called when user logs out or app is destroyed.
     */
    fun cleanup() {
        scope.launch {
            activeChannels.values.forEach { channel ->
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
            activeChannels.clear()
        }
    }

    private fun monitorNetworkChanges() {
        scope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _connectionState.value = if (isConnected) {
                    RealtimeConnectionState.Connected
                } else {
                    RealtimeConnectionState.Disconnected
                }
            }
        }
    }
}

sealed class RealtimeConnectionState {
    object Connected : RealtimeConnectionState()
    object Disconnected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
