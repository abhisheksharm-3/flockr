package `in`.xroden.flockr.features.shopping.model

import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * DTO for shopping item with nested profile data from database query.
 * Automatically deserialized by Supabase SDK.
 */
@Serializable
internal data class ShoppingItemWithProfiles(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    val quantity: String? = null,
    @SerialName("is_purchased")
    val isPurchased: Boolean = false,
    @SerialName("added_by")
    val addedBy: String? = null,
    @SerialName("added_by_profile")
    val addedByProfile: ProfileName? = null,
    @SerialName("purchased_by")
    val purchasedBy: String? = null,
    @SerialName("purchased_by_profile")
    val purchasedByProfile: ProfileName? = null,
    @SerialName("purchased_at")
    @Serializable(with = InstantSerializer::class)
    val purchasedAt: Instant? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
) {
    fun toShoppingItem() = ShoppingItem(
        id = id,
        houseId = houseId,
        itemName = itemName,
        quantity = quantity,
        isPurchased = isPurchased,
        addedBy = addedBy,
        addedByName = addedByProfile?.fullName,
        purchasedBy = purchasedBy,
        purchasedByName = purchasedByProfile?.fullName,
        purchasedAt = purchasedAt,
        createdAt = createdAt
    )
}

@Serializable
internal data class ProfileName(
    @SerialName("full_name")
    val fullName: String
)
