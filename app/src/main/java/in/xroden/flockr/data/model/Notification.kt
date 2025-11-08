package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("house_id")
    val houseId: String? = null,
    val title: String,
    val message: String,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("data")
    val data: String? = null, // JSON string containing type and other metadata
    @SerialName("created_at")
    val createdAt: String
) {
    // Helper to parse the type from data JSON
    val notificationType: String?
        get() = data?.let { jsonString ->
            try {
                // Simple parsing for {"type":"expense"}
                val typeMatch = Regex(""""type"\s*:\s*"([^"]+)"""").find(jsonString)
                typeMatch?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }
        }
}

@Serializable
data class NotificationPreference(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("enable_member_joined")
    val enableMemberJoined: Boolean = true,
    @SerialName("enable_expense_added")
    val enableExpenseAdded: Boolean = true,
    @SerialName("enable_chore_assigned")
    val enableChoreAssigned: Boolean = true,
    @SerialName("enable_message_sent")
    val enableMessageSent: Boolean = true,
    @SerialName("enable_shopping_item_added")
    val enableShoppingItemAdded: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

