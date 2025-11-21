package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InvitationStatus {
    @SerialName("pending")
    PENDING,
    
    @SerialName("accepted")
    ACCEPTED,
    
    @SerialName("rejected")
    REJECTED
}


