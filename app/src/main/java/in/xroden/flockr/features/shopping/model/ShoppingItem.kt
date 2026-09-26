/** The house shopping list and the aisles it is grouped by. */
package `in`.xroden.flockr.features.shopping.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.core.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The aisles the list groups items by, in the order a shop is usually walked. */
val SHOPPING_CATEGORIES = listOf(
    "Produce", "Bakery", "Dairy & eggs", "Meat & fish", "Pantry", "Frozen", "Drinks", "Household", "Personal care", "Other",
)

/** [quantity] is free text such as "2 kg" or "a dozen". [category] is null for items added without one. */
@Immutable
@Serializable
data class ShoppingItem(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    val quantity: String? = null,
    val category: String? = null,
    @SerialName("is_purchased")
    val isPurchased: Boolean = false,
    @SerialName("added_by")
    val addedBy: String,
    @SerialName("purchased_by")
    val purchasedBy: String? = null,
    @SerialName("purchased_at")
    @Serializable(with = InstantSerializer::class)
    val purchasedAt: Instant? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
