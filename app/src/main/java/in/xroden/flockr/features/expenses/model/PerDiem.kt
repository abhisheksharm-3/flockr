package `in`.xroden.flockr.features.expenses.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.data.serialization.InstantSerializer
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Immutable
@Serializable
data class PerDiemConfig(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
    val category: String,
    val unit: String = "unit",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Immutable
@Serializable
data class PerDiemEntry(
    val id: String,
    @SerialName("config_id")
    val configId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @SerialName("added_by")
    val addedBy: String,
    val notes: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Immutable
@Serializable
data class PerDiemBillItemized(
    @SerialName("item_name")
    val itemName: String,
    val category: String,
    val unit: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
    @SerialName("total_quantity")
    @Serializable(with = BigDecimalSerializer::class)
    val totalQuantity: BigDecimal,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)

@Immutable
@Serializable
data class PerDiemBillByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("total_quantity")
    @Serializable(with = BigDecimalSerializer::class)
    val totalQuantity: BigDecimal,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)

@Immutable
@Serializable
data class PerDiemEntryWithDetails(
    @SerialName("entry_id")
    val entryId: String,
    @SerialName("item_name")
    val itemName: String,
    val category: String,
    val unit: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @SerialName("total_cost")
    @Serializable(with = BigDecimalSerializer::class)
    val totalCost: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @SerialName("added_by")
    val addedBy: String,
    @SerialName("user_name")
    val userName: String,
    val notes: String?,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
