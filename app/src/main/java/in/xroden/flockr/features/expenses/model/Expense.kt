package `in`.xroden.flockr.features.expenses.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimeExpense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val name: String,
    val amount: Double,
    val category: String,
    @SerialName("paid_by")
    val paidBy: String,
    val date: String,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class RecurringExpense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val name: String,
    val amount: Double,
    @SerialName("due_day")
    val dueDay: Int,
    val category: String,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String,
    // Enhanced fields (to be added to database)
    val frequency: String = "monthly", // daily, weekly, biweekly, monthly, quarterly, semiannual, annual, custom
    @SerialName("next_due_date")
    val nextDueDate: String? = null,
    @SerialName("last_paid_date")
    val lastPaidDate: String? = null,
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean = true,
    val notes: String? = null,
    // Split functionality
    @SerialName("split_with")
    val splitWith: List<String>? = null, // Array of user IDs to split with
    @SerialName("split_type")
    val splitType: String? = null, // "equal" or "custom"
    @SerialName("split_amounts")
    val splitAmounts: Map<String, Double>? = null, // Custom amounts: {"user_id": amount}
    // Prepay and custom payment date
    @SerialName("prepay_enabled")
    val prepayEnabled: Boolean = false,
    @SerialName("first_payment_date")
    val firstPaymentDate: String? = null,
    @SerialName("next_payment_date")
    val nextPaymentDate: String? = null,
    // Computed fields from RPC
    @SerialName("due_status")
    val dueStatus: String? = null,
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
    val amountOwed: Double,
    @SerialName("is_settled")
    val isSettled: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
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
    val amount: Double,
    @SerialName("is_settlement")
    val isSettlement: Boolean = false,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class PaymentHistory(
    val id: String,
    @SerialName("recurring_expense_id")
    val recurringExpenseId: String,
    @SerialName("paid_by")
    val paidBy: String,
    val amount: Double,
    @SerialName("payment_date")
    val paymentDate: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class UserBalance(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("full_name")
    val fullName: String? = null,
    val balance: Double = 0.0
)

@Serializable
data class MonthlySummary(
    @SerialName("total_expenses")
    val totalExpenses: Double,
    @SerialName("recurring_expenses")
    val recurringExpenses: Double,
    @SerialName("one_time_expenses")
    val oneTimeExpenses: Double,
    @SerialName("per_diem_expenses")
    val perDiemExpenses: Double
)

@Serializable
data class SpendByMember(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("total_spent")
    val totalSpent: Double
)

@Serializable
data class SpendByCategory(
    val category: String,
    @SerialName("total_amount")
    val totalAmount: Double
)

