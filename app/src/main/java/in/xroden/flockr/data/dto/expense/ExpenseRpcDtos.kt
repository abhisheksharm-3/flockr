package `in`.xroden.flockr.data.dto.expense

import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

/** Parameters for creating an expense via RPC. */
@Serializable
data class CreateExpenseParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_paid_by") val paidBy: String,
    @SerialName("p_name") val name: String,
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_category") val category: String,
    @SerialName("p_date") val date: LocalDate,
    @SerialName("p_notes") val notes: String?,
    @SerialName("p_splits") val splits: JsonElement
)

/** Parameters for getting recurring expenses. */
@Serializable
data class GetRecurringExpensesParams(
    @SerialName("p_house_id") val houseId: String
)

/** Parameters for settling a balance. */
@Serializable
data class SettleBalanceParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_payer_id") val payerId: String,
    @SerialName("p_payee_id") val payeeId: String,
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_description") val description: String?
)

/** Parameters for getting user balances. */
@Serializable
data class GetUserBalancesParams(
    @SerialName("p_house_id") val houseId: String
)

/** Parameters for getting debt breakdown between users. */
@Serializable
data class GetDebtBreakdownParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_payer_id") val payerId: String,
    @SerialName("p_payee_id") val payeeId: String
)

/** Parameters for getting monthly summary. */
@Serializable
data class GetMonthlySummaryParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/** Parameters for getting spend by member. */
@Serializable
data class GetSpendByMemberParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/** Parameters for getting spend by category. */
@Serializable
data class GetSpendByCategoryParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/** Response item for debt breakdown RPC call. */
@Serializable
data class DebtBreakdownItem(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("expense_name") val expenseName: String,
    @SerialName("date") val date: LocalDate,
    @SerialName("amount_owed")
    @Serializable(with = BigDecimalSerializer::class)
    val amountOwed: BigDecimal,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)
