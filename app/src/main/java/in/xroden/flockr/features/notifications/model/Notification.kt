package `in`.xroden.flockr.features.notifications.model

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
                    // If it's a string primitive, return its content
                    if (jsonElement.isString) {
                        jsonElement.content
                    } else {
                        jsonElement.toString()
                    }
                }
                is JsonObject -> {
                    // If it's a JSON object, convert it to string
                    jsonElement.toString()
                }
                else -> {
                    jsonElement.toString()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationSerializer", "Error deserializing data field", e)
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
    @SerialName("data")
    @Serializable(with = FlexibleDataSerializer::class)
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

