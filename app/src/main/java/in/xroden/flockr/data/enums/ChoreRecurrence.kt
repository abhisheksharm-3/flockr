package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChoreRecurrence {
    @SerialName("daily")
    DAILY,
    
    @SerialName("weekly")
    WEEKLY,
    
    @SerialName("monthly")
    MONTHLY,
    
    @SerialName("yearly")
    YEARLY
}


