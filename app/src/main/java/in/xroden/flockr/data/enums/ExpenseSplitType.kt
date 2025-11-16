package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExpenseSplitType {
    @SerialName("equal")
    EQUAL,
    
    @SerialName("custom")
    CUSTOM
}


