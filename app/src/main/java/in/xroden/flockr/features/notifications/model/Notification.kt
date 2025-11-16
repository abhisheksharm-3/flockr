package `in`.xroden.flockr.features.notifications.model

import `in`.xroden.flockr.data.enums.NotificationType
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Custom serializer for notification data field that can handle both:
 * - JSON string: "{\"type\":\"expense\"}"
 * - JSON object: {"type":"expense"}
 * - null
 */
object FlexibleDataSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("FlexibleData", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        return try {
            val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())
            when (jsonElement) {
                is JsonPrimitive -> {
                    if (jsonElement.isString) {
                        jsonElement.content
                    } else {
                        jsonElement.toString()
                    }
                }
                is JsonObject -> {
                    jsonElement.toString()
                }
                else -> {
                    jsonElement.toString()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

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
    @SerialName("type")
    val type: NotificationType = NotificationType.GENERAL,
    @SerialName("data")
    @Serializable(with = FlexibleDataSerializer::class)
    val data: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

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
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null
)
