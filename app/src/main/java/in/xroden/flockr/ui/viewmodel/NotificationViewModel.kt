package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Notification
import `in`.xroden.flockr.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.getNotificationsFlow().collect { notifications ->
                    _uiState.value = NotificationUiState.Success(notifications)
                    // Update unread count in real-time when notifications change
                    _unreadCount.value = notifications.count { !it.isRead }
                }
            } catch (e: Exception) {
                _uiState.value = NotificationUiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    private fun loadUnreadCount() {
        // This is now handled in loadNotifications() for real-time updates
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            loadUnreadCount()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            loadUnreadCount()
        }
    }
}

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(val notifications: List<Notification>) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

