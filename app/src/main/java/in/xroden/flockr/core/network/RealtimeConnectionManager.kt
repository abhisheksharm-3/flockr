package `in`.xroden.flockr.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Centralized manager for Supabase realtime connections with lifecycle awareness. */
@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    private val channelMutex = Mutex()

    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    init {
        monitorNetworkChanges()
    }

    /** Gets existing channel or creates new one. Thread-safe. */
    suspend fun getOrCreateChannel(channelId: String): RealtimeChannel = channelMutex.withLock {
        activeChannels.getOrPut(channelId) { supabase.realtime.channel(channelId) }
    }

    /** Removes and cleans up a channel by ID. */
    suspend fun removeChannel(channelId: String) {
        channelMutex.withLock {
            activeChannels.remove(channelId)
        }?.let { channel ->
            scope.launch { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }

    /** Removes and cleans up a channel instance. */
    suspend fun removeChannel(channel: RealtimeChannel) {
        val channelId = channelMutex.withLock {
            activeChannels.entries.find { it.value == channel }?.also {
                activeChannels.remove(it.key)
            }?.key
        }
        if (channelId != null) {
            scope.launch { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }

    /** Cleans up all channels. Call on app termination. */
    fun cleanup() {
        scope.launch {
            channelMutex.withLock {
                activeChannels.values.forEach { channel ->
                    runCatching { supabase.realtime.removeChannel(channel) }
                }
                activeChannels.clear()
            }
        }
    }

    /** Cancels scope and releases resources. Call when manager is no longer needed. */
    fun destroy() {
        cleanup()
        job.cancel()
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
    data object Connected : RealtimeConnectionState()
    data object Disconnected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
