package `in`.xroden.flockr.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PerDiemConfig(
    val id: String,
    val houseId: String,
    val itemName: String,
    val rate: Double,
    val category: String,
    val unit: String = "unit",
    val isActive: Boolean = true,
    val createdAt: String
)

@Serializable
data class PerDiemEntry(
    val id: String,
    val configId: String,
    val quantity: Double,
    val date: String,
    val addedBy: String,
    val notes: String? = null,
    val createdAt: String
)

@Serializable
data class PerDiemBillItemized(
    val itemName: String,
    val totalQuantity: Double,
    val rate: Double,
    val unit: String,
    val totalAmount: Double
)

@Serializable
data class PerDiemBillByMember(
    val userId: String,
    val fullName: String?,
    val totalQuantity: Double,
    val totalAmount: Double
)

