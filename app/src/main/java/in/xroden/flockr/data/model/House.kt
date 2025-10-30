package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class House(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("address")
    val address: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class HouseMember(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("joined_at")
    val joinedAt: String
)

@Serializable
data class HouseWithMembers(
    val house: House,
    val members: List<Profile>
)
