/** The house ledger: expenses and payments, recurring bills, balances and spending reports. */
package `in`.xroden.flockr.features.expenses.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.expenses.model.ExpenseDueStatus
import `in`.xroden.flockr.features.expenses.model.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.core.serialization.BigDecimalSerializer
import `in`.xroden.flockr.core.serialization.InstantSerializer
import `in`.xroden.flockr.core.serialization.LocalDateSerializer
import java.math.BigDecimal
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ExpenseKind {
    @SerialName("expense")
    EXPENSE,

    /** A payment from one housemate to another that settles what one owes the other. */
    @SerialName("settlement")
    SETTLEMENT
}

/**
 * One person's part in an expense. Across an expense, paid shares and owed shares each add up to
 * its amount, which the database enforces, so a person's balance is simply what they paid minus
 * what they owe. [splitValue] is what the split method was given for them: an exact amount, a
 * percentage or a number of shares; it is null for an equal split.
 */
@Immutable
@Serializable
data class ExpenseShare(
    @SerialName("user_id")
    val userId: String,
    @SerialName("paid_share")
    @Serializable(with = BigDecimalSerializer::class)
    val paidShare: BigDecimal,
    @SerialName("owed_share")
    @Serializable(with = BigDecimalSerializer::class)
    val owedShare: BigDecimal,
    @SerialName("split_value")
    @Serializable(with = BigDecimalSerializer::class)
    val splitValue: BigDecimal? = null
) {
    /** What this expense moves this person's balance by: positive when others owe them. */
    val net: BigDecimal get() = paidShare - owedShare
}

/**
 * An expense or a settlement. [category] is null exactly for settlements, and [splitMethod] is null
 * when one person bears the whole cost. [recurringExpenseId] names the bill this pays, if any, and
 * [perDiemMonth] the month of usage this bills, which makes it read-only.
 */
@Immutable
@Serializable
data class Expense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val kind: ExpenseKind = ExpenseKind.EXPENSE,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val category: String? = null,
    @SerialName("split_method")
    val splitMethod: SplitMethod? = null,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val notes: String? = null,
    @SerialName("receipt_path")
    val receiptPath: String? = null,
    @SerialName("recurring_expense_id")
    val recurringExpenseId: String? = null,
    @SerialName("per_diem_month")
    @Serializable(with = LocalDateSerializer::class)
    val perDiemMonth: LocalDate? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @SerialName("expense_shares")
    val shares: List<ExpenseShare> = emptyList()
) {
    /** Who paid. The app records one payer per expense; the ledger allows several. */
    val payerId: String? get() = shares.maxByOrNull { it.paidShare }?.userId

    fun shareOf(userId: String): ExpenseShare? = shares.firstOrNull { it.userId == userId }
}

/** A person's split value on a recurring bill, applied afresh to each payment's amount. */
@Immutable
@Serializable
data class RecurringShare(
    @SerialName("user_id")
    val userId: String,
    @SerialName("split_value")
    @Serializable(with = BigDecimalSerializer::class)
    val splitValue: BigDecimal
)

/** A recurring bill, with where it stands against today in the house's time zone. */
@Immutable
@Serializable
data class RecurringExpense(
    val id: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val category: String,
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("first_due_date")
    @Serializable(with = LocalDateSerializer::class)
    val firstDueDate: LocalDate,
    @SerialName("next_due_date")
    @Serializable(with = LocalDateSerializer::class)
    val nextDueDate: LocalDate,
    @SerialName("last_paid_date")
    @Serializable(with = LocalDateSerializer::class)
    val lastPaidDate: LocalDate? = null,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean = true,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("allow_prepayment")
    val allowPrepayment: Boolean = false,
    @SerialName("split_method")
    val splitMethod: SplitMethod? = null,
    val notes: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    val shares: List<RecurringShare> = emptyList(),
    @SerialName("due_status")
    val dueStatus: ExpenseDueStatus,
    @SerialName("days_until_due")
    val daysUntilDue: Int
)

/**
 * A member's standing in the house. [net] is [paid] minus [owed]: positive when the house owes
 * them, negative when they owe the house. Past members appear only while they are not square.
 */
@Serializable
data class MemberBalance(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val paid: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val owed: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val net: BigDecimal
)

/** One payment in the fewest payments that settle everyone up. */
@Serializable
data class SettleUpPayment(
    @SerialName("from_user_id")
    val fromUserId: String,
    @SerialName("to_user_id")
    val toUserId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal
)

/** Everyone's balance with the payments that would settle them. */
@Serializable
data class HouseStanding(val balances: List<MemberBalance>, val plan: List<SettleUpPayment>) {
    fun netOf(userId: String): BigDecimal = balances.firstOrNull { it.userId == userId }?.net ?: BigDecimal.ZERO

    /** The payments in [plan] that [userId] makes or receives. */
    fun paymentsOf(userId: String): List<SettleUpPayment> = plan.filter { it.fromUserId == userId || it.toUserId == userId }
}

/**
 * An expense or payment the viewer shares with one other member. [betweenUs] is how much it added to
 * what that member owes the viewer, negative when it added to what the viewer owes them, so the
 * entries sum to the balance between the two.
 */
@Serializable
data class SharedHistoryEntry(
    @SerialName("expense_id")
    val expenseId: String,
    val kind: ExpenseKind,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("between_us")
    @Serializable(with = BigDecimalSerializer::class)
    val betweenUs: BigDecimal
)

/** A calendar month's spending, excluding payments between housemates. */
@Serializable
data class MonthlySummary(
    @SerialName("total_spend")
    @Serializable(with = BigDecimalSerializer::class)
    val totalSpend: BigDecimal,
    @SerialName("recurring_spend")
    @Serializable(with = BigDecimalSerializer::class)
    val recurringSpend: BigDecimal,
    @SerialName("one_time_spend")
    @Serializable(with = BigDecimalSerializer::class)
    val oneTimeSpend: BigDecimal,
    @SerialName("per_diem_spend")
    @Serializable(with = BigDecimalSerializer::class)
    val perDiemSpend: BigDecimal
)

/** What a member paid for the house in a month and what their own share of it came to. */
@Serializable
data class SpendByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val paid: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val consumed: BigDecimal
)

@Serializable
data class SpendByCategory(
    val category: String,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal
)
