package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.PaymentHistory
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/**
 * Repository interface for recurring expense operations.
 * Enables easy mocking for unit tests.
 */
interface IRecurringExpenseRepository {
    fun getRecurringExpensesFlow(houseId: String): Flow<Result<List<RecurringExpense>>>
    suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>>
    suspend fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        dueDay: Int,
        category: String,
        frequency: ExpenseFrequency,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int,
        reminderEnabled: Boolean,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?,
        prepayEnabled: Boolean,
        firstPaymentDate: LocalDate?
    ): Result<RecurringExpense>
    suspend fun updateRecurringExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        dueDay: Int?,
        category: String?,
        isActive: Boolean?,
        frequency: ExpenseFrequency?,
        lastPaidDate: LocalDate?,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int?,
        reminderEnabled: Boolean?,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit>
    suspend fun deleteRecurringExpense(expenseId: String): Result<Unit>
    suspend fun markRecurringExpenseAsPaid(
        expenseId: String,
        amount: BigDecimal,
        paymentDate: LocalDate
    ): Result<Unit>
    suspend fun getPaymentHistory(recurringExpenseId: String): Result<List<PaymentHistory>>
}
