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
    CUSTOM;

    fun toDisplayName(): String {
        return when (this) {
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            BIWEEKLY -> "Bi-weekly"
            MONTHLY -> "Monthly"
            QUARTERLY -> "Quarterly"
            SEMIANNUAL -> "Semi-annual"
            ANNUAL -> "Annual"
            CUSTOM -> "Custom"
        }
    }
}


