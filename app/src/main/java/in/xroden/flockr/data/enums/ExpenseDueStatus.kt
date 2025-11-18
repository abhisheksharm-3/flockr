package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExpenseDueStatus {
    @SerialName("not_set")
    NOT_SET,
    
    @SerialName("pending")
    PENDING,
    
    @SerialName("overdue")
    OVERDUE,
    
    @SerialName("due_today")
    DUE_TODAY,
    
    @SerialName("due_soon")
    DUE_SOON,
    
    @SerialName("upcoming")
    UPCOMING,
    
    @SerialName("scheduled")
    SCHEDULED
}


