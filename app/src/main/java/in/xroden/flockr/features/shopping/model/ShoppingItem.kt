package `in`.xroden.flockr.features.shopping.model

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
    @SerialName("added_by")
    val addedBy: String?,
    @SerialName("added_by_name")
    val addedByName: String? = null,
    @SerialName("purchased_by")
    val purchasedBy: String? = null,
    @SerialName("purchased_by_name")
    val purchasedByName: String? = null,
    @SerialName("purchased_at")
    val purchasedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

