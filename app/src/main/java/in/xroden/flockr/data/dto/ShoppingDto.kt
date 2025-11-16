package `in`.xroden.flockr.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItemInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    val quantity: String? = null,
    @SerialName("added_by")
    val addedBy: String
)

@Serializable
data class ShoppingItemUpdate(
    @SerialName("item_name")
    val itemName: String? = null,
    val quantity: String? = null,
    @SerialName("is_purchased")
    val isPurchased: Boolean? = null,
    @SerialName("purchased_by")
    val purchasedBy: String? = null
)


