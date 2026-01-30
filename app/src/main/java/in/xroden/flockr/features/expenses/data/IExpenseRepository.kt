package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/**
 * Repository interface for expense operations.
 * Enables easy mocking for unit tests.
 */
interface IExpenseRepository {
    fun getOneTimeExpensesFlow(houseId: String): Flow<Result<List<OneTimeExpense>>>
    suspend fun getOneTimeExpense(expenseId: String): Result<OneTimeExpense>
    suspend fun createOneTimeExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        category: String,
        paidBy: String,
        date: LocalDate,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        customAmounts: Map<String, BigDecimal>?
    ): Result<Unit>
    suspend fun updateOneTimeExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        category: String?,
        date: LocalDate?,
        notes: String?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit>
    suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit>
    fun getCurrentUserId(): String?
}
