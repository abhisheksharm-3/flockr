package `in`.xroden.flockr.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RecurringExpense(
    val id: String,
    val houseId: String,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val category: String,
    val createdBy: String,
    val isActive: Boolean = true,
    val createdAt: String
)

@Serializable
data class OneTimeExpense(
    val id: String,
    val houseId: String,
    val name: String,
    val amount: Double,
    val date: String,
    val paidBy: String,
    val category: String,
    val notes: String? = null,
    val createdAt: String
)

@Serializable
data class ExpenseSplit(
    val id: String,
    val expenseId: String,
    val userId: String,
    val amountOwed: Double,
    val isSettled: Boolean = false,
    val createdAt: String
)

@Serializable
data class Transaction(
    val id: String,
    val houseId: String,
    val payerId: String,
    val payeeId: String,
    val amount: Double,
    val isSettlement: Boolean = false,
    val description: String? = null,
    val createdAt: String
)

@Serializable
data class UserBalance(
    val userId: String,
    val fullName: String?,
    val balance: Double
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

