package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.CreateExpenseParams
import `in`.xroden.flockr.data.dto.ExpenseSplitInsert
import `in`.xroden.flockr.data.dto.OneTimeExpenseUpdate
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import `in`.xroden.flockr.data.util.createRealtimeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton
import `in`.xroden.flockr.core.security.InputSanitizer


@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IExpenseRepository {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getCurrentUserId(): String? = userId

    // ONE-TIME EXPENSES

    override fun getOneTimeExpensesFlow(houseId: String): Flow<Result<List<OneTimeExpense>>> =
        createRealtimeFlow(
            supabase = supabase,
            channelPrefix = "one_time_expenses",
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
        val currentUserId = paidBy.ifEmpty { userId ?: throw IllegalStateException("No user logged in") }

        // Sanitize user inputs
        val sanitizedName = InputSanitizer.sanitizeText(name)
        val sanitizedCategory = InputSanitizer.sanitizeText(category)
        val sanitizedNotes = notes?.let { InputSanitizer.sanitizeText(it) }

        // Construct Splits JSON Array
        val splitsJson = buildSplitsJson(
            amount = amount,
            currentUserId = currentUserId,
            splitWith = splitWith,
            splitType = splitType,
            splitAmounts = customAmounts
        )

        val expenseId = supabase.postgrest.rpc(
            function = "create_one_time_expense",
            parameters = CreateExpenseParams(
                houseId = houseId,
                paidBy = currentUserId,
                name = sanitizedName,
                amount = amount,
                category = sanitizedCategory,
                date = date,
                notes = sanitizedNotes,
                splits = splitsJson
            )
        ).decodeAs<String>()

        // No need to return anything for Unit
    }

    private fun buildSplitsJson(
        amount: BigDecimal,
        currentUserId: String,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ) = buildJsonArray {
        if (!splitWith.isNullOrEmpty()) {
            when (splitType) {
                ExpenseSplitType.EQUAL -> {
                    // Unique participants including payer
                    val uniqueParticipants = (splitWith + currentUserId).distinct()
                    val totalPeople = uniqueParticipants.size
                    val splitAmount = amount.divide(BigDecimal(totalPeople), 2, RoundingMode.HALF_UP)

                    // Add splits for everyone EXCEPT payer
                    uniqueParticipants
                        .filter { it != currentUserId }
                        .forEach { participantId ->
                            add(buildJsonObject {
                                put("user_id", participantId)
                                put("amount", splitAmount.toDouble())
                            })
                        }
                }
                ExpenseSplitType.AMOUNT -> {
                    splitAmounts?.forEach { (splitUserId, splitAmount) ->
                        if (splitUserId != currentUserId) {
                            add(buildJsonObject {
                                put("user_id", splitUserId)
                                put("amount", splitAmount.toDouble())
                            })
                        }
                    }
                }
                else -> { /* No splits */ }
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
        // Sanitize user inputs
        val sanitizedName = name?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedCategory = category?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedNotes = notes?.let { InputSanitizer.sanitizeText(it) }
        
        supabase.from("one_time_expenses")
            .update(
                OneTimeExpenseUpdate(
                    name = sanitizedName,
                    amount = amount,
                    date = date,
                    category = sanitizedCategory,
                    notes = sanitizedNotes
                )
            ) {
                filter { eq("id", expenseId) }
            }

        if (splitAmounts != null) {
            // Delete existing splits
            supabase.from("expense_splits").delete {
                filter { eq("expense_id", expenseId) }
            }

            val currentUserId = userId
            val validSplits = splitAmounts.filter { (splitUserId, _) ->
                splitUserId != currentUserId
            }.map { (splitUserId, amountOwed) ->
                ExpenseSplitInsert(
                    expenseId = expenseId,
                    userId = splitUserId,
                    amountOwed = amountOwed
                )
            }

            if (validSplits.isNotEmpty()) {
                supabase.from("expense_splits").insert(validSplits)
            }
        }
    }

    override suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit> = runCatching {
        // Cascade delete splits first (though DB FK might handle it, stricter here)
        supabase.from("expense_splits").delete {
            filter { eq("expense_id", expenseId) }
        }

        supabase.from("one_time_expenses").delete {
            filter { eq("id", expenseId) }
        }
    }
}
