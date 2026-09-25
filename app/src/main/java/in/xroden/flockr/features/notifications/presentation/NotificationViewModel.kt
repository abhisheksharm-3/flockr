/** The inbox: the member's notifications, kept current, with reading and clearing them. */
package `in`.xroden.flockr.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.system.NotificationPoster
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Error(val message: String) : NotificationUiState
    data class Ready(val notifications: List<Notification>) : NotificationUiState {
        val unreadCount: Int get() = notifications.count { !it.isRead }
    }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val poster: NotificationPoster,
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getNotificationsFlow().collect { result ->
                _state.value = result.fold(
                    onSuccess = { NotificationUiState.Ready(it) },
                    onFailure = { NotificationUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    /** Marks [notification] read and removes it from the shade; the live query confirms it. */
    fun open(notification: Notification) {
        poster.cancel(notification.id)
        if (notification.isRead) return
        optimistically { list -> list.map { if (it.id == notification.id) it.copy(isRead = true) else it } }
        viewModelScope.launch { repository.markRead(listOf(notification.id)) }
    }

    /** Finds the notification a system notification tap named, marks it read, and returns it to navigate by. */
    suspend fun openById(id: String): Notification? =
        repository.getNotification(id).getOrNull()?.also(::open)

    fun markAllRead() {
        optimistically { list -> list.map { it.copy(isRead = true) } }
        viewModelScope.launch { repository.markRead(null) }
    }

    fun delete(notification: Notification) {
        poster.cancel(notification.id)
        optimistically { list -> list.filterNot { it.id == notification.id } }
        viewModelScope.launch { repository.delete(notification.id) }
    }

    fun clearRead() {
        optimistically { list -> list.filterNot { it.isRead } }
        viewModelScope.launch { repository.deleteRead() }
    }

    private fun optimistically(change: (List<Notification>) -> List<Notification>) = _state.update { state ->
        (state as? NotificationUiState.Ready)?.let { NotificationUiState.Ready(change(it.notifications)) } ?: state
    }
}
