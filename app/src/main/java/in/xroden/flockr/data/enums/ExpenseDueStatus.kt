package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExpenseDueStatus {
    @SerialName("pending")
    PENDING,
    
    @SerialName("overdue")
    OVERDUE,
    
    @SerialName("due_today")
    DUE_TODAY,
    
    @SerialName("upcoming")
    UPCOMING,
    
    @SerialName("scheduled")
    SCHEDULED
}


