package `in`.xroden.flockr.features.notifications.presentation

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

    private var notificationJob: kotlinx.coroutines.Job? = null
    private var isFirstLoad = true

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        notificationJob?.cancel()
        notificationJob = viewModelScope.launch {
            // Only show loading on first load
            if (isFirstLoad) {
                _uiState.value = NotificationUiState.Loading
                isFirstLoad = false
            }

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
            notificationRepository.markAsRead(notificationId).fold(
                onSuccess = {
                    // Optimistically update UI
                    val currentState = _uiState.value
                    if (currentState is NotificationUiState.Success) {
                        val updated = currentState.notifications.map { 
                            if (it.id == notificationId) it.copy(isRead = true) else it 
                        }
                        _uiState.value = currentState.copy(
                            notifications = updated,
                            unreadCount = updated.count { !it.isRead }
                        )
                    }
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

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead().fold(
                onSuccess = {
                    // Optimistically update UI
                    val currentState = _uiState.value
                    if (currentState is NotificationUiState.Success) {
                        val updated = currentState.notifications.map { it.copy(isRead = true) }
                        _uiState.value = currentState.copy(
                            notifications = updated,
                            unreadCount = 0
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = NotificationUiState.Error(
                        message = error.message ?: "Failed to mark all as read",
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
                    // Optimistically update UI
                    val currentState = _uiState.value
                    if (currentState is NotificationUiState.Success) {
                        val updated = currentState.notifications.filter { it.id != notificationId }
                        _uiState.value = currentState.copy(
                            notifications = updated,
                            unreadCount = updated.count { !it.isRead }
                        )
                    }
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

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.deleteAllNotifications().fold(
                onSuccess = {
                    // Optimistically update UI
                    val currentState = _uiState.value
                    if (currentState is NotificationUiState.Success) {
                        _uiState.value = currentState.copy(
                            notifications = emptyList(),
                            unreadCount = 0
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = NotificationUiState.Error(
                        message = error.message ?: "Failed to clear notifications",
                        cause = error
                    )
                }
            )
        }
    }
}
