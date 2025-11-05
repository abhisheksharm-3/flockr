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

