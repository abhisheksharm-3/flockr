package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val frequency: String = "Monthly",
    @SerialName("next_payment_date")
    val nextPaymentDate: String = "",
    @SerialName("is_paid")
    val isPaid: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class OneTimeExpense(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    val name: String,
    val amount: Double,
    val date: String,
    @SerialName("paid_by")
    val paidBy: String,
    val category: String,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String
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
data class UserBalance(
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("full_name")
    val fullName: String? = null,
    val balance: Double = 0.0
)

@Serializable
data class MonthlySummary(
    val totalExpenses: Double,
    val recurringExpenses: Double,
    val oneTimeExpenses: Double,
    val perDiemExpenses: Double
)

@Serializable
data class SpendByMember(
    val userId: String,
    val fullName: String?,
    val totalSpent: Double
)

@Serializable
data class SpendByCategory(
    val category: String,
    val totalAmount: Double
)

