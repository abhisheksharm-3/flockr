package `in`.xroden.flockr.features.expenses.model

import `in`.xroden.flockr.data.enums.ExpenseDueStatus
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.data.serialization.InstantSerializer
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.math.BigDecimal

@Serializable
data class OneTimeExpense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val category: String,
    @SerialName("paid_by")
    val paidBy: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val notes: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Serializable
data class RecurringExpense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("due_day")
    val dueDay: Int,
    val category: String,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
    @SerialName("next_due_date")
    @Serializable(with = LocalDateSerializer::class)
    val nextDueDate: LocalDate? = null,
    @SerialName("last_paid_date")
    @Serializable(with = LocalDateSerializer::class)
    val lastPaidDate: LocalDate? = null,
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean = true,
    val notes: String? = null,
    @SerialName("split_with")
    val splitWith: List<String>? = null,
    @SerialName("split_type")
    val splitType: ExpenseSplitType? = null,
    @SerialName("split_amounts")
    val splitAmounts: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>? = null,
    @SerialName("prepay_enabled")
    val prepayEnabled: Boolean = false,
    @SerialName("first_payment_date")
    @Serializable(with = LocalDateSerializer::class)
    val firstPaymentDate: LocalDate? = null,
    @SerialName("next_payment_date")
    @Serializable(with = LocalDateSerializer::class)
    val nextPaymentDate: LocalDate? = null,
    @SerialName("due_status")
    val dueStatus: ExpenseDueStatus? = null,
    @SerialName("days_until_due")
    val daysUntilDue: Int? = null
)

@Serializable
data class ExpenseSplit(
    val id: String,
    @SerialName("expense_id")
    val expenseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("amount_owed")
    @Serializable(with = BigDecimalSerializer::class)
    val amountOwed: BigDecimal,
    @SerialName("is_settled")
    val isSettled: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Serializable
data class Transaction(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("payer_id")
    val payerId: String,
    @SerialName("payee_id")
    val payeeId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("is_settlement")
    val isSettlement: Boolean = false,
    val description: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Serializable
data class PaymentHistory(
    val id: String,
    @SerialName("recurring_expense_id")
    val recurringExpenseId: String,
    @SerialName("paid_by")
    val paidBy: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("payment_date")
    @Serializable(with = LocalDateSerializer::class)
    val paymentDate: LocalDate,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Serializable
data class UserBalance(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("full_name")
    val fullName: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal = BigDecimal.ZERO
)

@Serializable
data class MonthlySummary(
    @SerialName("total_expenses")
    @Serializable(with = BigDecimalSerializer::class)
    val totalExpenses: BigDecimal,
    @SerialName("recurring_expenses")
    @Serializable(with = BigDecimalSerializer::class)
    val recurringExpenses: BigDecimal,
    @SerialName("one_time_expenses")
    @Serializable(with = BigDecimalSerializer::class)
    val oneTimeExpenses: BigDecimal,
    @SerialName("per_diem_expenses")
    @Serializable(with = BigDecimalSerializer::class)
    val perDiemExpenses: BigDecimal
)

@Serializable
data class SpendByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("total_spent")
    @Serializable(with = BigDecimalSerializer::class)
    val totalSpent: BigDecimal
)

@Serializable
data class SpendByCategory(
    val category: String,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)
