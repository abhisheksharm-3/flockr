package `in`.xroden.flockr.features.notifications.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
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

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            
            notificationRepository.getNotificationsFlow().collect { result ->
                result.fold(
                    onSuccess = { notifications ->
                        val unreadCount = notifications.count { !it.isRead }
                        _uiState.value = NotificationUiState.Success(
                            notifications = notifications,
                            unreadCount = unreadCount
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = NotificationUiState.Error(
                            message = error.message ?: "Failed to load notifications",
                            cause = error
                        )
                    }
                )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationAsRead(notificationId).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = NotificationUiState.Error(
                        message = error.message ?: "Failed to mark as read",
                        cause = error
                    )
                }
            )
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = NotificationUiState.Error(
                        message = error.message ?: "Failed to delete notification",
                        cause = error
                    )
                }
            )
        }
    }
}
