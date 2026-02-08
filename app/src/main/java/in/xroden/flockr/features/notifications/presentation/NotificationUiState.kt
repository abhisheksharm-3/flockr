package `in`.xroden.flockr.features.notifications.presentation

import `in`.xroden.flockr.features.notifications.model.Notification

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int
    ) : NotificationUiState
    data class Error(val message: String, val cause: Throwable? = null) : NotificationUiState
}


