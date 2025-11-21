package `in`.xroden.flockr.domain.usecase.notification

import `in`.xroden.flockr.data.enums.NotificationType
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import javax.inject.Inject

/**
 * Use case to filter notifications based on user preferences
 */
class FilterNotificationsByPreferencesUseCase @Inject constructor() {

    /**
     * Filter notifications based on user preferences
     * 
     * @param notifications List of all notifications
     * @param preferences User's notification preferences
     * @return Filtered list of notifications user wants to see
     */
    operator fun invoke(
        notifications: List<Notification>,
        preferences: NotificationPreference
    ): List<Notification> {
        return notifications.filter { notification ->
            when (notification.type) {
                NotificationType.MEMBER_JOINED -> preferences.enableMemberJoined
                NotificationType.EXPENSE -> preferences.enableExpenseAdded
                NotificationType.CHORE_ASSIGNED -> preferences.enableChoreAssigned
                NotificationType.MESSAGE -> preferences.enableMessageSent
                NotificationType.SHOPPING_ITEM -> preferences.enableShoppingItemAdded
                else -> true // Always show other types (general, house_invite, etc.)
            }
        }
    }

    /**
     * Get count of unread notifications after filtering
     */
    fun getUnreadCount(
        notifications: List<Notification>,
        preferences: NotificationPreference
    ): Int {
        return invoke(notifications, preferences).count { !it.isRead }
    }
}


