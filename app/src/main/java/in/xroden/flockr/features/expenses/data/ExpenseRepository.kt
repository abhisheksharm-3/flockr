package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.ExpenseSplitInsert
import `in`.xroden.flockr.data.dto.OneTimeExpenseInsert
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
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getOneTimeExpenses(houseId: String): Result<List<OneTimeExpense>> {
        return try {
            val expenses = supabase.from("one_time_expenses")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<OneTimeExpense>()

            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOneTimeExpense(expenseId: String): Result<OneTimeExpense> {
        return try {
            val expense = supabase.from("one_time_expenses")
                .select(columns = Columns.list("*, expense_splits(*)")) {
                    filter {
                        eq("id", expenseId)
                    }
                }
                .decodeSingle<OneTimeExpense>()

            Result.success(expense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOneTimeExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        category: String,
        date: LocalDate,
        notes: String?,
        splitWith: List<String>? = null,
        splitType: `in`.xroden.flockr.data.enums.ExpenseSplitType? = null,
        splitAmounts: Map<String, BigDecimal>? = null
    ): Result<OneTimeExpense> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            // Construct Splits JSON Array
            val splitsJson = buildJsonArray {
                if (!splitWith.isNullOrEmpty()) {
                    if (splitType == `in`.xroden.flockr.data.enums.ExpenseSplitType.EQUAL) {
                        // Fix: Calculate unique participants including payer to avoid double counting if payer is also in splitWith
                        val uniqueParticipants = (splitWith + currentUserId).distinct()
                        val totalPeople = uniqueParticipants.size
                        
                        val splitAmount = amount.divide(BigDecimal(totalPeople), 2, java.math.RoundingMode.HALF_UP)
                        
                        // Add splits for everyone EXCEPT payer (payer's share is implicit/residual)
                        uniqueParticipants.forEach { participantId ->
                            if (participantId != currentUserId) {
                                add(buildJsonObject {
                                    put("user_id", participantId)
                                    put("amount", splitAmount.toDouble())
                                })
                            }
                        }
                    } else if (splitType == `in`.xroden.flockr.data.enums.ExpenseSplitType.AMOUNT && splitAmounts != null) {
                        splitAmounts.forEach { (splitUserId, splitAmount) ->
                             if (splitUserId != currentUserId) {
                                 add(buildJsonObject {
                                     put("user_id", splitUserId)
                                     put("amount", splitAmount.toDouble())
                                 })
                             }
                        }
                    }
                }
            }

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

            // Call RPC
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
            
            // Re-fetch the created expense to return full object (backward compatibility with UI)
            getOneTimeExpense(expenseId)

        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "createOneTimeExpense failed", e)
            Result.failure(e)
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
    ): Result<Unit> {
        return try {
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit> {
        return try {
            supabase.from("expense_splits")
                .delete {
                    filter { eq("expense_id", expenseId) }
                }

            supabase.from("one_time_expenses")
                .delete {
                    filter { eq("id", expenseId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>> {
        return try {
            @Serializable
            data class GetRecurringExpensesParams(
                @SerialName("p_house_id")
                val houseId: String
            )

            val expenses = supabase.postgrest.rpc(
                function = "get_recurring_expenses_with_status",
                parameters = GetRecurringExpensesParams(houseId = houseId)
            ).decodeList<RecurringExpense>()

            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        splitType: `in`.xroden.flockr.data.enums.ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?,
        prepayEnabled: Boolean,
        firstPaymentDate: LocalDate?
    ): Result<RecurringExpense> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val expense = supabase.from("recurring_expenses")
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

            Result.success(expense)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        splitType: `in`.xroden.flockr.data.enums.ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit> {
        return try {
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringExpense(expenseId: String): Result<Unit> {
        return try {
            supabase.from("payment_history")
                .delete {
                    filter { eq("recurring_expense_id", expenseId) }
                }

            supabase.from("recurring_expenses")
                .delete {
                    filter { eq("id", expenseId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markRecurringExpenseAsPaid(
        expenseId: String,
        amount: BigDecimal,
        paymentDate: LocalDate
    ): Result<Unit> {
        return try {
            android.util.Log.d("ExpenseRepository", "markRecurringExpenseAsPaid called - expenseId: $expenseId, amount: $amount, date: $paymentDate")
            
            val currentUserId = userId ?: run {
                android.util.Log.e("ExpenseRepository", "markRecurringExpenseAsPaid failed - No user logged in")
                return Result.failure(Exception("No user logged in"))
            }
            
            // 1. Fetch the recurring expense to get details (splits, category, etc.)
            val recurringExpense = supabase.from("recurring_expenses")
                .select(Columns.list("*, split_with, split_type, split_amounts")) {
                    filter { eq("id", expenseId) }
                }
                .decodeSingleOrNull<RecurringExpense>() ?: return Result.failure(Exception("Recurring expense not found"))

            // 2. Insert payment history (keep this for record)
            supabase.from("payment_history")
                .insert(
                    PaymentHistoryInsert(
                        recurringExpenseId = expenseId,
                        paidBy = currentUserId,
                        amount = amount,
                        paymentDate = paymentDate
                    )
                )

            // 3. Create a OneTimeExpense to represent this payment (so it affects balances and shows in history)
            // We use the createOneTimeExpense function to handle splits and notifications automatically
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
            )

            // 4. Update recurring expense with last paid date
            supabase.from("recurring_expenses")
                .update(
                    RecurringExpenseUpdate(
                        lastPaidDate = paymentDate
                    )
                ) {
                    filter { eq("id", expenseId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "markRecurringExpenseAsPaid failed with exception", e)
            Result.failure(e)
        }
    }

    suspend fun getPaymentHistory(recurringExpenseId: String): Result<List<PaymentHistory>> {
        return try {
            val history = supabase.from("payment_history")
                .select(Columns.ALL) {
                    filter {
                        eq("recurring_expense_id", recurringExpenseId)
                    }
                    order("payment_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<PaymentHistory>()

            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getTransactions(houseId: String): Result<List<Transaction>> {
        return try {
            val transactions = supabase.from("transactions")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Transaction>()

            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTransaction(
        houseId: String,
        payerId: String,
        payeeId: String,
        amount: BigDecimal,
        isSettlement: Boolean,
        description: String?
    ): Result<Transaction> {
        return try {
            val transaction = supabase.from("transactions")
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

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            supabase.from("transactions")
                .delete {
                    filter { eq("id", transactionId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
    ): Result<Unit> {
        return try {
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

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "settleBalance failed", e)
            Result.failure(e)
        }
    }

    // ANALYTICS & SUMMARIES

    suspend fun getUserBalances(houseId: String): Result<List<UserBalance>> {
        return try {
            @Serializable
            data class GetUserBalancesParams(
                @SerialName("p_house_id")
                val houseId: String
            )

            val balances = supabase.postgrest.rpc(
                function = "get_user_balances",
                parameters = GetUserBalancesParams(houseId = houseId)
            ).decodeList<UserBalance>()

            Result.success(balances)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDebtBreakdown(houseId: String, payerId: String, payeeId: String): Result<List<DebtBreakdownItem>> {
        return try {
            @Serializable
            data class GetDebtBreakdownParams(
                @SerialName("p_house_id") val houseId: String,
                @SerialName("p_payer_id") val payerId: String,
                @SerialName("p_payee_id") val payeeId: String
            )

            val breakdown = supabase.postgrest.rpc(
                function = "get_debt_breakdown",
                parameters = GetDebtBreakdownParams(
                    houseId = houseId,
                    payerId = payerId,
                    payeeId = payeeId
                )
            ).decodeList<DebtBreakdownItem>()

            Result.success(breakdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlySummary(houseId: String, month: String): Result<MonthlySummary> {
        return try {
            android.util.Log.d("ExpenseRepository", "getMonthlySummary called - houseId: $houseId, month: $month")
            
            @Serializable
            data class GetMonthlySummaryParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                val month: String
            )

            // RPC returns an array with a single object, not a single object directly
            val summaryList = supabase.postgrest.rpc(
                function = "get_monthly_summary",
                parameters = GetMonthlySummaryParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeList<MonthlySummary>()

            val summary = summaryList.firstOrNull() ?: throw Exception("No summary data returned from RPC")
            
            android.util.Log.d("ExpenseRepository", "getMonthlySummary result: totalExpenses=${summary.totalExpenses}, oneTimeExpenses=${summary.oneTimeExpenses}, recurringExpenses=${summary.recurringExpenses}, perDiemExpenses=${summary.perDiemExpenses}")
            Result.success(summary)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "getMonthlySummary failed", e)
            android.util.Log.e("ExpenseRepository", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getSpendByMember(houseId: String, month: String): Result<List<SpendByMember>> {
        return try {
            android.util.Log.d("ExpenseRepository", "getSpendByMember called - houseId: $houseId, month: $month")
            @Serializable
            data class GetSpendByMemberParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                val month: String
            )

            val spending = supabase.postgrest.rpc(
                function = "get_spend_by_member",
                parameters = GetSpendByMemberParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeList<SpendByMember>()

            android.util.Log.d("ExpenseRepository", "getSpendByMember result: ${spending.size} members, data: $spending")
            Result.success(spending)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "getSpendByMember failed", e)
            Result.failure(e)
        }
    }

    suspend fun getSpendByCategory(houseId: String, month: String): Result<List<SpendByCategory>> {
        return try {
            @Serializable
            data class GetSpendByCategoryParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                val month: String
            )

            val spending = supabase.postgrest.rpc(
                function = "get_spend_by_category",
                parameters = GetSpendByCategoryParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeList<SpendByCategory>()

            Result.success(spending)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
