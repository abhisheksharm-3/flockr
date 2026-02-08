package `in`.xroden.flockr.features.chat.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Message(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @SerialName("sender_name")
    val senderName: String? = null,
    @kotlinx.serialization.Transient
    val isPending: Boolean = false
)

@Serializable
data class ProfileName(
    @SerialName("full_name")
    val fullName: String? = null
)

@Serializable
data class MessageWithProfile(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    val content: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val profiles: ProfileName? = null
) {
    fun toMessage() = Message(
        id = id,
        houseId = houseId,
        userId = userId,
        content = content,
        createdAt = createdAt,
        senderName = profiles?.fullName
    )
}

