package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExpenseFrequency {
    @SerialName("daily")
    DAILY,
    
    @SerialName("weekly")
    WEEKLY,
    
    @SerialName("biweekly")
    BIWEEKLY,
    
    @SerialName("monthly")
    MONTHLY,
    
    @SerialName("quarterly")
    QUARTERLY,
    
    @SerialName("semiannual")
    SEMIANNUAL,
    
    @SerialName("annual")
    ANNUAL,
    
    @SerialName("custom")
    CUSTOM
}


