package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    fun getNotificationsFlow(): Flow<Result<List<Notification>>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun deleteAllNotifications(): Result<Unit>
    suspend fun getNotificationPreferences(houseId: String): Result<NotificationPreference?>
    suspend fun getNotificationPreferences(): List<NotificationPreference>
    suspend fun ensurePreferencesExist(houseId: String)
    suspend fun updateNotificationPreferences(houseId: String, key: String, enabled: Boolean): Result<Unit>
}
