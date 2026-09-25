/** What someone holding an invite code sees of the house before joining it. */
package `in`.xroden.flockr.features.house.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HousePreview(
    val id: String,
    val name: String,
    @SerialName("header_image_url") val headerImageUrl: String? = null,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("is_full") val isFull: Boolean
)
