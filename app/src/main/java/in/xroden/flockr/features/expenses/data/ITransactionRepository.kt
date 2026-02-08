package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.features.expenses.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

interface ITransactionRepository {
    fun getTransactionsFlow(houseId: String): Flow<Result<List<Transaction>>>
    suspend fun getTransactions(houseId: String): Result<List<Transaction>>
    suspend fun createTransaction(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        isSettlement: Boolean,
        description: String?
    ): Result<Transaction>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun settleBalance(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        date: LocalDate,
        name: String,
        notes: String?
    ): Result<Unit>
}
