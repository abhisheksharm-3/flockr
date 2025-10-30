package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    val quantity: String? = null,
    @SerialName("is_purchased")
    val isPurchased: Boolean = false,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    val createdAt: String
)

