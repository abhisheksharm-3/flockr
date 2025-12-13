package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.ExpenseSplitInsert
import `in`.xroden.flockr.data.dto.OneTimeExpenseUpdate
import `in`.xroden.flockr.data.dto.PaymentHistoryInsert
import `in`.xroden.flockr.data.dto.RecurringExpenseInsert
import `in`.xroden.flockr.data.dto.RecurringExpenseUpdate
import `in`.xroden.flockr.data.dto.TransactionInsert
import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.expenses.model.PaymentHistory
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.expenses.model.Transaction
import `in`.xroden.flockr.features.expenses.model.UserBalance
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import java.math.RoundingMode

@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    @Serializable
    data class DebtBreakdownItem(
        @SerialName("expense_id") val expenseId: String,
        @SerialName("expense_name") val expenseName: String,
        @SerialName("date") val date: LocalDate,
        @SerialName("amount_owed")
        @Serializable(with = BigDecimalSerializer::class)
        val amountOwed: BigDecimal,
        @SerialName("total_amount")
        @Serializable(with = BigDecimalSerializer::class)
        val totalAmount: BigDecimal
    )

    // ONE-TIME EXPENSES

    fun getOneTimeExpensesFlow(houseId: String): Flow<Result<List<OneTimeExpense>>> = callbackFlow {
        val channelId = "one_time_expenses_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getOneTimeExpenses(houseId))

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "one_time_expenses"
                filter = "house_id=eq.$houseId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getOneTimeExpenses(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            launch {
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }
    }

    suspend fun getOneTimeExpenses(houseId: String): Result<List<OneTimeExpense>> = runCatching {
        supabase.from("one_time_expenses")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<OneTimeExpense>()
    }

    suspend fun getOneTimeExpense(expenseId: String): Result<OneTimeExpense> = runCatching {
        supabase.from("one_time_expenses")
            .select(columns = Columns.list("*, expense_splits(*)")) {
                filter { eq("id", expenseId) }
            }
            .decodeSingle<OneTimeExpense>()
    }

    suspend fun createOneTimeExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        category: String,
        date: LocalDate,
        notes: String?,
        splitWith: List<String>? = null,
        splitType: ExpenseSplitType? = null,
        splitAmounts: Map<String, BigDecimal>? = null
    ): Result<OneTimeExpense> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        // Construct Splits JSON Array
        val splitsJson = buildSplitsJson(
            amount = amount,
            currentUserId = currentUserId,
            splitWith = splitWith,
            splitType = splitType,
            splitAmounts = splitAmounts
        )

        @Serializable
        data class CreateExpenseParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_paid_by") val paidBy: String,
            @SerialName("p_name") val name: String,
            @SerialName("p_amount")
            @Serializable(with = BigDecimalSerializer::class)
            val amount: BigDecimal,
            @SerialName("p_category") val category: String,
            @SerialName("p_date") val date: LocalDate,
            @SerialName("p_notes") val notes: String?,
            @SerialName("p_splits") val splits: JsonElement
        )

        val expenseId = supabase.postgrest.rpc(
            function = "create_one_time_expense",
            parameters = CreateExpenseParams(
                houseId = houseId,
                paidBy = currentUserId,
                name = name,
                amount = amount,
                category = category,
                date = date,
                notes = notes,
                splits = splitsJson
            )
        ).decodeAs<String>()

        // Re-fetch to return full object
        getOneTimeExpense(expenseId).getOrThrow()
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

    suspend fun updateOneTimeExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        date: LocalDate?,
        category: String?,
        notes: String?,
        splitAmounts: Map<String, BigDecimal>? = null
    ): Result<Unit> = runCatching {
        supabase.from("one_time_expenses")
            .update(
                OneTimeExpenseUpdate(
                    name = name,
                    amount = amount,
                    date = date,
                    category = category,
                    notes = notes
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

    suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit> = runCatching {
        // Cascade delete splits first (though DB FK might handle it, stricter here)
        supabase.from("expense_splits").delete {
            filter { eq("expense_id", expenseId) }
        }

        supabase.from("one_time_expenses").delete {
            filter { eq("id", expenseId) }
        }
    }

    // RECURRING EXPENSES

    fun getRecurringExpensesFlow(houseId: String): Flow<Result<List<RecurringExpense>>> = callbackFlow {
        val channelId = "recurring_expenses_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getRecurringExpenses(houseId))

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "recurring_expenses"
                filter = "house_id=eq.$houseId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getRecurringExpenses(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            launch {
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }
    }

    suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>> = runCatching {
        @Serializable
        data class GetRecurringExpensesParams(
            @SerialName("p_house_id") val houseId: String
        )

        supabase.postgrest.rpc(
            function = "get_recurring_expenses_with_status",
            parameters = GetRecurringExpensesParams(houseId = houseId)
        ).decodeList<RecurringExpense>()
    }

    suspend fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        dueDay: Int,
        category: String,
        frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int,
        reminderEnabled: Boolean,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?,
        prepayEnabled: Boolean,
        firstPaymentDate: LocalDate?
    ): Result<RecurringExpense> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        supabase.from("recurring_expenses")
            .insert(
                RecurringExpenseInsert(
                    houseId = houseId,
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    createdBy = currentUserId,
                    frequency = frequency,
                    customFrequencyDays = customFrequencyDays,
                    reminderDaysBefore = reminderDaysBefore,
                    reminderEnabled = reminderEnabled,
                    notes = notes,
                    splitWith = splitWith,
                    splitType = splitType,
                    splitAmounts = splitAmounts,
                    prepayEnabled = prepayEnabled,
                    firstPaymentDate = firstPaymentDate
                )
            ) {
                select()
            }
            .decodeSingle<RecurringExpense>()
    }

    suspend fun updateRecurringExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        dueDay: Int?,
        category: String?,
        isActive: Boolean?,
        frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency?,
        lastPaidDate: LocalDate?,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int?,
        reminderEnabled: Boolean?,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit> = runCatching {
        supabase.from("recurring_expenses")
            .update(
                RecurringExpenseUpdate(
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    isActive = isActive,
                    frequency = frequency,
                    lastPaidDate = lastPaidDate,
                    customFrequencyDays = customFrequencyDays,
                    reminderDaysBefore = reminderDaysBefore,
                    reminderEnabled = reminderEnabled,
                    notes = notes,
                    splitWith = splitWith,
                    splitType = splitType,
                    splitAmounts = splitAmounts
                )
            ) {
                filter { eq("id", expenseId) }
            }
    }

    suspend fun deleteRecurringExpense(expenseId: String): Result<Unit> = runCatching {
        // Delete history first
        supabase.from("payment_history").delete {
            filter { eq("recurring_expense_id", expenseId) }
        }

        supabase.from("recurring_expenses").delete {
            filter { eq("id", expenseId) }
        }
    }

    suspend fun markRecurringExpenseAsPaid(
        expenseId: String,
        amount: BigDecimal,
        paymentDate: LocalDate
    ): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        // 1. Fetch details
        val recurringExpense = supabase.from("recurring_expenses")
            .select(Columns.list("*, split_with, split_type, split_amounts")) {
                filter { eq("id", expenseId) }
            }
            .decodeSingleOrNull<RecurringExpense>() ?: throw IllegalStateException("Recurring expense not found")

        // 2. Insert payment history
        supabase.from("payment_history")
            .insert(
                PaymentHistoryInsert(
                    recurringExpenseId = expenseId,
                    paidBy = currentUserId,
                    amount = amount,
                    paymentDate = paymentDate
                )
            )

        // 3. Create OneTimeExpense for balances
        createOneTimeExpense(
            houseId = recurringExpense.houseId,
            name = recurringExpense.name,
            amount = amount,
            category = recurringExpense.category,
            date = paymentDate,
            notes = "Recurring Payment",
            splitWith = recurringExpense.splitWith,
            splitType = recurringExpense.splitType,
            splitAmounts = recurringExpense.splitAmounts
        ).getOrThrow()

        // 4. Update last paid date
        supabase.from("recurring_expenses")
            .update(RecurringExpenseUpdate(lastPaidDate = paymentDate)) {
                filter { eq("id", expenseId) }
            }
    }

    suspend fun getPaymentHistory(recurringExpenseId: String): Result<List<PaymentHistory>> = runCatching {
        supabase.from("payment_history")
            .select(Columns.ALL) {
                filter { eq("recurring_expense_id", recurringExpenseId) }
                order("payment_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<PaymentHistory>()
    }

    // TRANSACTIONS & SETTLEMENTS

    fun getTransactionsFlow(houseId: String): Flow<Result<List<Transaction>>> = callbackFlow {
        val channelId = "transactions_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getTransactions(houseId))

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "transactions"
                filter = "house_id=eq.$houseId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getTransactions(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            launch {
                runCatching {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }
    }

    suspend fun getTransactions(houseId: String): Result<List<Transaction>> = runCatching {
        supabase.from("transactions")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<Transaction>()
    }

    suspend fun createTransaction(
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

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
        supabase.from("transactions")
            .delete {
                filter { eq("id", transactionId) }
            }
    }

    suspend fun settleBalance(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        date: LocalDate,
        name: String,
        notes: String?
    ): Result<Unit> = runCatching {
        @Serializable
        data class SettleBalanceParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_payer_id") val payerId: String,
            @SerialName("p_payee_id") val payeeId: String,
            @SerialName("p_amount")
            @Serializable(with = BigDecimalSerializer::class)
            val amount: BigDecimal,
            @SerialName("p_description") val description: String?
        )

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

    // ANALYTICS & SUMMARIES

    suspend fun getUserBalances(houseId: String): Result<List<UserBalance>> = runCatching {
        @Serializable
        data class GetUserBalancesParams(
            @SerialName("p_house_id") val houseId: String
        )

        supabase.postgrest.rpc(
            function = "get_user_balances",
            parameters = GetUserBalancesParams(houseId = houseId)
        ).decodeList<UserBalance>()
    }

    suspend fun getDebtBreakdown(houseId: String, payerId: String, payeeId: String): Result<List<DebtBreakdownItem>> = runCatching {
        @Serializable
        data class GetDebtBreakdownParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_payer_id") val payerId: String,
            @SerialName("p_payee_id") val payeeId: String
        )

        supabase.postgrest.rpc(
            function = "get_debt_breakdown",
            parameters = GetDebtBreakdownParams(
                houseId = houseId,
                payerId = payerId,
                payeeId = payeeId
            )
        ).decodeList<DebtBreakdownItem>()
    }

    suspend fun getMonthlySummary(houseId: String, month: String): Result<MonthlySummary> = runCatching {
        @Serializable
        data class GetMonthlySummaryParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month") val month: String
        )

        val summaryList = supabase.postgrest.rpc(
            function = "get_monthly_summary",
            parameters = GetMonthlySummaryParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<MonthlySummary>()

        summaryList.firstOrNull() ?: throw IllegalStateException("No summary data returned from RPC")
    }

    suspend fun getSpendByMember(houseId: String, month: String): Result<List<SpendByMember>> = runCatching {
        @Serializable
        data class GetSpendByMemberParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month") val month: String
        )

        supabase.postgrest.rpc(
            function = "get_spend_by_member",
            parameters = GetSpendByMemberParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<SpendByMember>()
    }

    suspend fun getSpendByCategory(houseId: String, month: String): Result<List<SpendByCategory>> = runCatching {
        @Serializable
        data class GetSpendByCategoryParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month") val month: String
        )

        supabase.postgrest.rpc(
            function = "get_spend_by_category",
            parameters = GetSpendByCategoryParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<SpendByCategory>()
    }
}
