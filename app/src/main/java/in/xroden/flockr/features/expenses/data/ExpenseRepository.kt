package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.ExpenseSplitInsert
import `in`.xroden.flockr.data.dto.OneTimeExpenseUpdate
import `in`.xroden.flockr.data.dto.expense.CreateExpenseParams
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager
) : BaseRealtimeRepository(supabase, connectionManager), IExpenseRepository {

    override fun getCurrentUserId(): String? = authenticatedUserId

    override fun getOneTimeExpensesFlow(houseId: String): Flow<Result<List<OneTimeExpense>>> =
        createRealtimeFlow(
            channelId = "one_time_expenses_$houseId",
            table = "one_time_expenses",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getOneTimeExpenses(houseId) }
        )

    suspend fun getOneTimeExpenses(houseId: String): Result<List<OneTimeExpense>> = runCatching {
        supabase.from("one_time_expenses")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<OneTimeExpense>()
    }

    override suspend fun getOneTimeExpense(expenseId: String): Result<OneTimeExpense> = runCatching {
        supabase.from("one_time_expenses")
            .select(columns = Columns.list("*, expense_splits(*)")) {
                filter { eq("id", expenseId) }
            }
            .decodeSingle<OneTimeExpense>()
    }

    override suspend fun createOneTimeExpense(
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
    ): Result<Unit> = runCatching {
        val resolvedUserId = paidBy.ifEmpty { requireAuthenticated(authenticatedUserId) }
        val sanitizedName = InputSanitizer.sanitizeText(name)
        val sanitizedCategory = InputSanitizer.sanitizeText(category)
        val sanitizedNotes = notes?.let { InputSanitizer.sanitizeText(it) }

        val splitsJson = buildSplitsJson(
            amount = amount,
            payerId = resolvedUserId,
            splitWith = splitWith,
            splitType = splitType,
            splitAmounts = customAmounts
        )

        supabase.postgrest.rpc(
            function = "create_one_time_expense",
            parameters = CreateExpenseParams(
                houseId = houseId,
                paidBy = resolvedUserId,
                name = sanitizedName,
                amount = amount,
                category = sanitizedCategory,
                date = date,
                notes = sanitizedNotes,
                splits = splitsJson
            )
        )
    }

    private fun buildSplitsJson(
        amount: BigDecimal,
        payerId: String,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ) = buildJsonArray {
        if (!splitWith.isNullOrEmpty()) {
            when (splitType) {
                ExpenseSplitType.EQUAL -> {
                    val uniqueParticipants = (splitWith + payerId).distinct()
                    val splitAmount = amount.divide(BigDecimal(uniqueParticipants.size), 2, RoundingMode.HALF_UP)
                    uniqueParticipants.filter { it != payerId }.forEach { participantId ->
                        add(buildJsonObject {
                            put("user_id", participantId)
                            put("amount", splitAmount.toDouble())
                        })
                    }
                }
                ExpenseSplitType.AMOUNT -> {
                    splitAmounts?.forEach { (splitUserId, splitAmount) ->
                        if (splitUserId != payerId) {
                            add(buildJsonObject {
                                put("user_id", splitUserId)
                                put("amount", splitAmount.toDouble())
                            })
                        }
                    }
                }
                else -> { }
            }
        }
    }

    override suspend fun updateOneTimeExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        category: String?,
        date: LocalDate?,
        notes: String?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit> = runCatching {
        val sanitizedName = name?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedCategory = category?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedNotes = notes?.let { InputSanitizer.sanitizeText(it) }
        
        supabase.from("one_time_expenses")
            .update(OneTimeExpenseUpdate(
                name = sanitizedName,
                amount = amount,
                date = date,
                category = sanitizedCategory,
                notes = sanitizedNotes
            )) {
                filter { eq("id", expenseId) }
            }

        if (splitAmounts != null) {
            supabase.from("expense_splits").delete { filter { eq("expense_id", expenseId) } }

            val validSplits = splitAmounts.filter { (splitUserId, _) ->
                splitUserId != authenticatedUserId
            }.map { (splitUserId, amountOwed) ->
                ExpenseSplitInsert(expenseId = expenseId, userId = splitUserId, amountOwed = amountOwed)
            }

            if (validSplits.isNotEmpty()) {
                supabase.from("expense_splits").insert(validSplits)
            }
        }
    }

    override suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit> = runCatching {
        supabase.from("expense_splits").delete { filter { eq("expense_id", expenseId) } }
        supabase.from("one_time_expenses").delete { filter { eq("id", expenseId) } }
    }
}
