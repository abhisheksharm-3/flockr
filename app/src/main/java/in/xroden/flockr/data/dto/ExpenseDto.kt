package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OneTimeExpenseInsert(
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
    val notes: String? = null
)

@Serializable
data class OneTimeExpenseUpdate(
    val name: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    val category: String? = null,
    @SerialName("paid_by")
    val paidBy: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val notes: String? = null
)

@Serializable
data class RecurringExpenseInsert(
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
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
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
    val firstPaymentDate: LocalDate? = null
)

@Serializable
data class RecurringExpenseUpdate(
    val name: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    @SerialName("due_day")
    val dueDay: Int? = null,
    val category: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    val frequency: ExpenseFrequency? = null,
    @SerialName("last_paid_date")
    @Serializable(with = LocalDateSerializer::class)
    val lastPaidDate: LocalDate? = null,
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int? = null,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean? = null,
    val notes: String? = null,
    @SerialName("split_with")
    val splitWith: List<String>? = null,
    @SerialName("split_type")
    val splitType: ExpenseSplitType? = null,
    @SerialName("split_amounts")
    val splitAmounts: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>? = null
)

@Serializable
data class PaymentHistoryInsert(
    @SerialName("recurring_expense_id")
    val recurringExpenseId: String,
    @SerialName("paid_by")
    val paidBy: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("payment_date")
    @Serializable(with = LocalDateSerializer::class)
    val paymentDate: LocalDate
)

@Serializable
data class ExpenseSplitInsert(
    @SerialName("expense_id")
    val expenseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("amount_owed")
    @Serializable(with = BigDecimalSerializer::class)
    val amountOwed: BigDecimal
)

@Serializable
data class TransactionInsert(
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
    val description: String? = null
)


