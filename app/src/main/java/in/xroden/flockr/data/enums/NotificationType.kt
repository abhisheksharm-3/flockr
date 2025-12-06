package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    @SerialName("general")
    GENERAL,
    
    @SerialName("expense")
    EXPENSE,
    
    @SerialName("expense_split")
    EXPENSE_SPLIT,
    
    @SerialName("settlement")
    SETTLEMENT,
    
    @SerialName("chore")
    CHORE,
    
    @SerialName("chore_assigned")
    CHORE_ASSIGNED,
    
    @SerialName("shopping")
    SHOPPING,
    
    @SerialName("shopping_item")
    SHOPPING_ITEM,
    
    @SerialName("per_diem")
    PER_DIEM,
    
    @SerialName("house_invitation")
    HOUSE_INVITE,
    
    @SerialName("member_joined")
    MEMBER_JOINED,
    
    @SerialName("message")
    MESSAGE,

    @SerialName("message_sent")
    MESSAGE_SENT,

    @SerialName("document")
    DOCUMENT
}


