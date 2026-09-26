/** Recurring bills: their schedule and split, and recording each payment as an expense. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.realtime.OfflineCache
import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.features.expenses.model.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.realtime.cachedAs
import `in`.xroden.flockr.core.realtime.liveQuery
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.RecurringShare
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

@Singleton
class RecurringExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    /** The house's bills, soonest due first. Recording a payment moves a bill on, so payments refresh it too. */
    fun getRecurringExpensesFlow(houseId: String): Flow<Result<List<RecurringExpense>>> =
        supabase.liveQuery(
            listOf(TableWatch("recurring_expenses", "house_id", houseId), TableWatch("expenses", "house_id", houseId)),
            cachedAs<List<RecurringExpense>>("bills_$houseId"),
        ) { fetchRecurringExpenses(houseId) }

    suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>> = runCatching {
        OfflineCache.fetchOrSaved(cachedAs<List<RecurringExpense>>("bills_$houseId")) { fetchRecurringExpenses(houseId) }
    }

    private suspend fun fetchRecurringExpenses(houseId: String): List<RecurringExpense> =
        supabase.postgrest.rpc("get_recurring_expenses", buildJsonObject { put("p_house_id", houseId) })
            .decodeList<RecurringExpense>()

    /**
     * Adds a bill, or replaces [billId]'s details and split, and returns its id. A bill's first due
     * date is fixed once it exists, because its schedule counts from there.
     */
    suspend fun saveRecurringExpense(
        houseId: String,
        billId: String?,
        name: String,
        amount: BigDecimal,
        category: String,
        frequency: ExpenseFrequency,
        customFrequencyDays: Int?,
        firstDueDate: LocalDate,
        reminderEnabled: Boolean,
        reminderDaysBefore: Int,
        allowPrepayment: Boolean,
        splitMethod: SplitMethod?,
        shares: List<RecurringShare>,
        notes: String?,
    ): Result<String> = runCatching {
        supabase.postgrest.rpc(
            "save_recurring_expense",
            buildJsonObject {
                put("p_recurring_id", billId)
                put("p_house_id", houseId)
                put("p_name", InputSanitizer.sanitizeText(name))
                put("p_amount", amount.toPlainString())
                put("p_category", category)
                put("p_frequency", Json.encodeToJsonElement(frequency))
                put("p_custom_frequency_days", customFrequencyDays?.takeIf { frequency == ExpenseFrequency.CUSTOM })
                put("p_first_due_date", firstDueDate.toString())
                put("p_reminder_enabled", reminderEnabled)
                put("p_reminder_days_before", reminderDaysBefore)
                put("p_allow_prepayment", allowPrepayment)
                put("p_split_method", splitMethod?.let { Json.encodeToJsonElement(it) } ?: JsonNull)
                put("p_shares", buildJsonArray {
                    shares.forEach { share ->
                        add(buildJsonObject {
                            put("user_id", share.userId)
                            put("split_value", share.splitValue.toPlainString())
                        })
                    }
                })
                put("p_notes", notes?.let(InputSanitizer::sanitizeText)?.ifBlank { null })
            }
        ).decodeAs<String>()
    }

    /** Deletes the bill. Payments already recorded stay in the ledger as ordinary expenses. */
    suspend fun deleteRecurringExpense(billId: String): Result<Unit> = runCatching {
        supabase.from("recurring_expenses").delete { filter { eq("id", billId) } }
    }

    /**
     * Records that the signed-in user paid [amount] towards [bill] on [date], split by the bill's
     * split, and moves the bill to its next due date.
     */
    suspend fun payRecurringExpense(bill: RecurringExpense, amount: BigDecimal, date: LocalDate, minorDigits: Int): Result<Unit> =
        runCatching {
            val payerId = requireAuthenticated(supabase.auth.currentUserOrNull()?.id)
            val shares = recurringPaymentShares(bill, amount, payerId, minorDigits)
                ?: throw DomainError.ValidationError.Rule("This amount can't be split the way the bill is")
            supabase.postgrest.rpc(
                "pay_recurring_expense",
                buildJsonObject {
                    put("p_recurring_id", bill.id)
                    put("p_amount", amount.toPlainString())
                    put("p_date", date.toString())
                    put("p_shares", sharesJson(shares))
                }
            )
        }

    /** The payments recorded against a bill, newest first. */
    suspend fun getBillPayments(billId: String): Result<List<Expense>> = runCatching {
        supabase.from("expenses").select(EXPENSE_WITH_SHARES) {
            filter { eq("recurring_expense_id", billId) }
            order("date", Order.DESCENDING)
        }.decodeList<Expense>()
    }
}
