/** Things the house pays for by usage, such as milk or water cans, and the usage recorded against them. */
package `in`.xroden.flockr.features.expenses.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.core.serialization.BigDecimalSerializer
import `in`.xroden.flockr.core.serialization.InstantSerializer
import `in`.xroden.flockr.core.serialization.LocalDateSerializer
import java.math.BigDecimal
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An item priced at [rate] per [unit]. An archived item ([isActive] false) keeps its history but takes no new entries. */
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

/**
 * One recorded use. [rate] is the item's price when it was recorded and [totalCost] is quantity
 * times that price rounded to the currency, both fixed by the database, so repricing an item never
 * changes a past bill.
 */
@Immutable
@Serializable
data class PerDiemEntryWithDetails(
    val id: String,
    @SerialName("config_id")
    val configId: String,
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
    @SerialName("added_by_name")
    val addedByName: String,
    val notes: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

/** A month's usage of one item, costed at whatever price each entry was recorded at. */
@Immutable
@Serializable
data class PerDiemBillItemized(
    @SerialName("config_id")
    val configId: String,
    @SerialName("item_name")
    val itemName: String,
    val category: String,
    val unit: String,
    @SerialName("total_quantity")
    @Serializable(with = BigDecimalSerializer::class)
    val totalQuantity: BigDecimal,
    @SerialName("total_cost")
    @Serializable(with = BigDecimalSerializer::class)
    val totalCost: BigDecimal
)

/** What a member's recorded usage came to in a month. */
@Immutable
@Serializable
data class PerDiemBillByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("total_cost")
    @Serializable(with = BigDecimalSerializer::class)
    val totalCost: BigDecimal
)

/** Everything the usage screen shows for one month. [usageBill] is set once the month has been billed. */
data class PerDiemMonth(
    val items: List<PerDiemConfig>,
    val entries: List<PerDiemEntryWithDetails>,
    val byMember: List<PerDiemBillByMember>,
    val usageBill: Expense?,
) {
    val total: BigDecimal get() = byMember.fold(BigDecimal.ZERO) { sum, member -> sum + member.totalCost }
}
