package `in`.xroden.flockr.features.house.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HouseMemberRole {
    @SerialName("Owner")
    OWNER,

    @SerialName("Admin")
    ADMIN,

    @SerialName("Member")
    MEMBER
}
