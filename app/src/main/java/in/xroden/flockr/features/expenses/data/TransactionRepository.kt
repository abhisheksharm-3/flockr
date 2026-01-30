package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.SettleBalanceParams
import `in`.xroden.flockr.data.dto.TransactionInsert
import `in`.xroden.flockr.features.expenses.model.Transaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager
) : BaseRealtimeRepository(supabase, connectionManager), ITransactionRepository {

    override fun getTransactionsFlow(houseId: String): Flow<Result<List<Transaction>>> {
        return createRealtimeFlow(
            channelId = "transactions_$houseId",
            table = "transactions",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getTransactions(houseId) }
        )
    }

    override suspend fun getTransactions(houseId: String): Result<List<Transaction>> = runCatching {
        supabase.from("transactions")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Transaction>()
    }

    override suspend fun createTransaction(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        isSettlement: Boolean,
        description: String?
    ): Result<Transaction> = runCatching {
        supabase.from("transactions")
            .insert(
                TransactionInsert(
                    houseId = houseId,
                    payerId = payerId,
                    payeeId = payeeId,
                    amount = amount,
                    isSettlement = isSettlement,
                    description = description
                )
            ) {
                select()
            }
            .decodeSingle<Transaction>()
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        supabase.from("transactions")
            .delete {
                filter { eq("id", transactionId) }
            }
    }

    override suspend fun settleBalance(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        date: LocalDate,
        name: String, // Kept for API compatibility, though not used in RPC
        notes: String?
    ): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "settle_balance",
            parameters = SettleBalanceParams(
                houseId = houseId,
                payerId = payerId,
                payeeId = payeeId,
                amount = amount,
                description = notes
            )
        )
    }
}
