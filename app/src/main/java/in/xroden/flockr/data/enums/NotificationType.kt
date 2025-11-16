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
    
    @SerialName("shopping")
    SHOPPING,
    
    @SerialName("per_diem")
    PER_DIEM,
    
    @SerialName("house_invite")
    HOUSE_INVITE
}


