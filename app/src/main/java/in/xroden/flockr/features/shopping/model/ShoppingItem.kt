package `in`.xroden.flockr.features.shopping.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
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
    @Serializable(with = InstantSerializer::class)
    val purchasedAt: Instant? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
