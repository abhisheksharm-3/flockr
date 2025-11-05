package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PerDiemConfig(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    val rate: Double,
    val category: String,
    val unit: String = "unit",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class PerDiemEntry(
    val id: String,
    @SerialName("config_id")
    val configId: String,
    val quantity: Double,
    val date: String,
    @SerialName("added_by")
    val addedBy: String,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class PerDiemBillItemized(
    @SerialName("item_name")
    val itemName: String,
    @SerialName("total_quantity")
    val totalQuantity: Double,
    val rate: Double,
    val unit: String,
    @SerialName("total_amount")
    val totalAmount: Double
)

@Serializable
data class PerDiemBillByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("total_quantity")
    val totalQuantity: Double,
    @SerialName("total_amount")
    val totalAmount: Double
)

@Serializable
data class PerDiemEntryWithDetails(
    @SerialName("entry_id")
    val entryId: String,
    @SerialName("item_name")
    val itemName: String,
    val category: String,
    val unit: String,
    val rate: Double,
    val quantity: Double,
    @SerialName("total_cost")
    val totalCost: Double,
    val date: String,
    @SerialName("added_by")
    val addedBy: String,
    @SerialName("user_name")
    val userName: String,
    val notes: String?,
    @SerialName("created_at")
    val createdAt: String
)

