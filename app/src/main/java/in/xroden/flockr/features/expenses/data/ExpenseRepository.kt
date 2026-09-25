/** The house ledger: expenses, payments between housemates, and the balances they add up to. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.data.enums.SplitMethod
import `in`.xroden.flockr.data.realtime.TableWatch
import `in`.xroden.flockr.data.realtime.liveQuery
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.model.HouseStanding
import `in`.xroden.flockr.features.expenses.model.MemberBalance
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.expenses.model.SharedHistoryEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/** An expense row with its shares embedded, the shape [Expense] decodes. */
internal val EXPENSE_WITH_SHARES = Columns.raw("*, expense_shares(user_id, paid_share, owed_share, split_value)")

@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val connectionManager: RealtimeConnectionManager,
) {
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Every expense and payment in the house, newest first, kept current as housemates add them. */
    fun getExpensesFlow(houseId: String): Flow<Result<List<Expense>>> =
        supabase.liveQuery(connectionManager, listOf(TableWatch("expenses", "house_id", houseId))) {
            supabase.from("expenses").select(EXPENSE_WITH_SHARES) {
                filter { eq("house_id", houseId) }
                order("date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }.decodeList<Expense>()
        }

    suspend fun getExpense(expenseId: String): Result<Expense> = runCatching {
        supabase.from("expenses").select(EXPENSE_WITH_SHARES) { filter { eq("id", expenseId) } }.decodeSingle<Expense>()
    }

    /**
     * Adds an expense, or replaces [expenseId]'s details and split, and returns its id. [shares] come
     * from [expenseShares]; the database refuses them unless they add up to [amount] exactly.
     */
    suspend fun saveExpense(
        houseId: String,
        expenseId: String?,
        name: String,
        amount: BigDecimal,
        category: String,
        date: LocalDate,
        notes: String?,
        splitMethod: SplitMethod?,
        shares: List<ExpenseShare>,
    ): Result<String> = runCatching {
        supabase.postgrest.rpc(
            "save_expense",
            buildJsonObject {
                put("p_expense_id", expenseId)
                put("p_house_id", houseId)
                put("p_name", InputSanitizer.sanitizeText(name))
                put("p_amount", amount.toPlainString())
                put("p_category", category)
                put("p_date", date.toString())
                put("p_notes", notes?.let(InputSanitizer::sanitizeText))
                put("p_split_method", splitMethod?.let { Json.encodeToJsonElement(it) } ?: JsonNull)
                put("p_shares", sharesJson(shares))
            }
        ).decodeAs<String>()
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("delete_expense", buildJsonObject { put("p_expense_id", expenseId) })
    }

    /**
     * Records that [fromUserId] paid [toUserId] back [amount]. The signed-in user must be one of the
     * two; the other is notified. Paying more than is owed carries forward as a balance the other way.
     */
    suspend fun settleUp(
        houseId: String,
        fromUserId: String,
        toUserId: String,
        amount: BigDecimal,
        date: LocalDate,
        note: String?,
    ): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "settle_up",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_from_user_id", fromUserId)
                put("p_to_user_id", toUserId)
                put("p_amount", amount.toPlainString())
                put("p_date", date.toString())
                put("p_note", note?.let(InputSanitizer::sanitizeText)?.ifBlank { null })
            }
        )
    }

    /** Everyone's balance and the fewest payments that would settle the house, read together so they agree. */
    suspend fun getStanding(houseId: String): Result<HouseStanding> = runCatching {
        coroutineScope {
            val params = buildJsonObject { put("p_house_id", houseId) }
            val balances = async { supabase.postgrest.rpc("get_balances", params).decodeList<MemberBalance>() }
            val plan = async { supabase.postgrest.rpc("get_settle_up_plan", params).decodeList<SettleUpPayment>() }
            HouseStanding(balances.await(), plan.await())
        }
    }

    suspend fun getSharedHistory(houseId: String, otherUserId: String): Result<List<SharedHistoryEntry>> = runCatching {
        supabase.postgrest.rpc(
            "get_shared_history",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_other_user_id", otherUserId)
            }
        ).decodeList<SharedHistoryEntry>()
    }
}

