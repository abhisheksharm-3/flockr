package `in`.xroden.flockr.features.house.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HousePreview(
    val id: String,
    val name: String,
    @SerialName("header_image_url") val headerImageUrl: String? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("member_count") val memberCount: Long? = null
)
