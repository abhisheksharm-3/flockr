/** The house ledger: expenses, payments between housemates, and the balances they add up to. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.realtime.OfflineCache
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.realtime.cachedAs
import `in`.xroden.flockr.core.realtime.liveQuery
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlin.time.Duration.Companion.hours
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

private const val RECEIPTS_BUCKET = "receipts"
private const val EXPORT_PAGE = 500

/**
 * [this] with the characters `ilike` treats as wildcards escaped, and with the commas, quotes and
 * parentheses that would break PostgREST's `or` filter syntax dropped.
 */
private fun String.asLikeLiteral(): String =
    filterNot { it in ",()\"" }.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/** An expense row with its shares embedded, the shape [Expense] decodes. */
internal val EXPENSE_WITH_SHARES = Columns.raw("*, expense_shares(user_id, paid_share, owed_share, split_value)")

@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /**
     * The house's newest [limit] expenses and payments, kept current as housemates add them. With a
     * [search], only those whose name or notes contain it. The unsearched first page is saved for
     * offline use; searches are not.
     */
    fun getExpensesFlow(houseId: String, limit: Int, search: String = ""): Flow<Result<List<Expense>>> =
        supabase.liveQuery(
            listOf(TableWatch("expenses", "house_id", houseId)),
            cachedAs<List<Expense>>("expenses_${houseId}_$limit").takeIf { search.isBlank() },
        ) { fetchExpenses(houseId, from = 0, count = limit, search = search) }

    /** Every expense and payment the house has recorded, newest first, read a page at a time. */
    suspend fun getAllExpenses(houseId: String): Result<List<Expense>> = runCatching {
        buildList {
            do {
                val page = fetchExpenses(houseId, from = size, count = EXPORT_PAGE, search = "")
                addAll(page)
            } while (page.size == EXPORT_PAGE)
        }
    }

    private suspend fun fetchExpenses(houseId: String, from: Int, count: Int, search: String): List<Expense> {
        val pattern = search.trim().takeIf { it.isNotEmpty() }?.let { "%${it.asLikeLiteral()}%" }
        return supabase.from("expenses").select(EXPENSE_WITH_SHARES) {
            filter {
                eq("house_id", houseId)
                if (pattern != null) or {
                    ilike("name", pattern)
                    ilike("notes", pattern)
                }
            }
            order("date", Order.DESCENDING)
            order("created_at", Order.DESCENDING)
            range(from.toLong(), (from + count - 1).toLong())
        }.decodeList<Expense>()
    }

    /**
     * Stores [photo] (already shrunk to a JPEG) as [expenseId]'s receipt, replacing any earlier one,
     * under the uploader's folder in the house's receipts.
     */
    suspend fun attachReceipt(houseId: String, expenseId: String, photo: ByteArray): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val path = "$houseId/$userId/${expenseId}_${System.currentTimeMillis()}.jpg"
        supabase.storage.from(RECEIPTS_BUCKET).upload(path, photo) { upsert = false }
        supabase.postgrest.rpc("set_expense_receipt", buildJsonObject {
            put("p_expense_id", expenseId)
            put("p_path", path)
        })
    }

    /** A link to [receiptPath] that works for an hour, for showing the picture. */
    suspend fun receiptUrl(receiptPath: String): Result<String> = runCatching {
        supabase.storage.from(RECEIPTS_BUCKET).createSignedUrl(receiptPath, 1.hours)
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
        OfflineCache.fetchOrSaved(cachedAs<HouseStanding>("standing_$houseId")) { fetchStanding(houseId) }
    }

    private suspend fun fetchStanding(houseId: String): HouseStanding =
        coroutineScope {
            val params = buildJsonObject { put("p_house_id", houseId) }
            val balances = async { supabase.postgrest.rpc("get_balances", params).decodeList<MemberBalance>() }
            val plan = async { supabase.postgrest.rpc("get_settle_up_plan", params).decodeList<SettleUpPayment>() }
            HouseStanding(balances.await(), plan.await())
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

