package `in`.xroden.flockr.features.chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String
)


