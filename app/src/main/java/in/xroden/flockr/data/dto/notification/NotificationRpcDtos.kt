package `in`.xroden.flockr.data.dto.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Parameters for house-wide notifications. */
@Serializable
data class HouseNotificationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_message") val message: String,
    @SerialName("p_type") val type: String,
    @SerialName("p_data") val data: String,
    @SerialName("p_exclude_user_id") val excludeUserId: String?
)

/** Parameters for single-user notifications. */
@Serializable
data class NotificationParams(
    @SerialName("user_id") val userId: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val message: String,
    val type: String,
    val data: String
)

/** Parameters for deleting a notification. */
@Serializable
data class DeleteNotificationParams(
    @SerialName("p_notification_id") val notificationId: String
)

/** Parameters for deleting all user notifications. */
@Serializable
data class DeleteAllNotificationsParams(
    @SerialName("p_user_id") val userId: String
)

/** Parameters for inserting a notification. */
@Serializable
data class NotificationInsertParams(
    @SerialName("user_id") val userId: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("is_read") val isRead: Boolean = false
)
