package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.enums.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationUpdate(
    @SerialName("is_read")
    val isRead: Boolean
)

@Serializable
data class NotificationPreferenceUpdate(
    @SerialName("enable_member_joined")
    val enableMemberJoined: Boolean? = null,
    @SerialName("enable_expense_added")
    val enableExpenseAdded: Boolean? = null,
    @SerialName("enable_chore_assigned")
    val enableChoreAssigned: Boolean? = null,
    @SerialName("enable_message_sent")
    val enableMessageSent: Boolean? = null,
    @SerialName("enable_shopping_item_added")
    val enableShoppingItemAdded: Boolean? = null
)


