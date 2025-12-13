package `in`.xroden.flockr.data.serialization

import `in`.xroden.flockr.data.enums.NotificationType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NotificationTypeSerializer : KSerializer<NotificationType> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("in.xroden.flockr.data.enums.NotificationType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NotificationType {
        val value = decoder.decodeString()
        return when {
            value == "general" -> NotificationType.GENERAL
            value == "expense" -> NotificationType.EXPENSE
            value == "expense_split" -> NotificationType.EXPENSE_SPLIT
            value == "settlement" -> NotificationType.SETTLEMENT
            value == "chore" -> NotificationType.CHORE
            value == "chore_assigned" -> NotificationType.CHORE_ASSIGNED
            value == "shopping" -> NotificationType.SHOPPING
            value == "shopping_item" -> NotificationType.SHOPPING_ITEM
            value == "per_diem" -> NotificationType.PER_DIEM
            value.startsWith("house_invitation") -> NotificationType.HOUSE_INVITE
            value == "member_joined" -> NotificationType.MEMBER_JOINED
            value == "message" -> NotificationType.MESSAGE
            value == "message_sent" -> NotificationType.MESSAGE_SENT
            value == "document" -> NotificationType.DOCUMENT
            else -> NotificationType.GENERAL
        }
    }

    override fun serialize(encoder: Encoder, value: NotificationType) {
        val stringValue = when (value) {
            NotificationType.GENERAL -> "general"
            NotificationType.EXPENSE -> "expense"
            NotificationType.EXPENSE_SPLIT -> "expense_split"
            NotificationType.SETTLEMENT -> "settlement"
            NotificationType.CHORE -> "chore"
            NotificationType.CHORE_ASSIGNED -> "chore_assigned"
            NotificationType.SHOPPING -> "shopping"
            NotificationType.SHOPPING_ITEM -> "shopping_item"
            NotificationType.PER_DIEM -> "per_diem"
            NotificationType.HOUSE_INVITE -> "house_invitation"
            NotificationType.MEMBER_JOINED -> "member_joined"
            NotificationType.MESSAGE -> "message"
            NotificationType.MESSAGE_SENT -> "message_sent"
            NotificationType.DOCUMENT -> "document"
        }
        encoder.encodeString(stringValue)
    }
}
