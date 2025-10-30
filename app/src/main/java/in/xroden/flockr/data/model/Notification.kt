package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("house_id")
    val houseId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("is_read")
    val isRead: Boolean = false,
    val data: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String
)

