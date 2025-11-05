package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.*
import `in`.xroden.flockr.utils.FlockrLogger
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id
    
    fun getCurrentUserId(): String? = userId
    
    companion object {
        private const val TAG = "ExpenseRepository"
    }

    suspend fun getOneTimeExpenses(houseId: String): List<OneTimeExpense> {
        FlockrLogger.repoStart(TAG, "getOneTimeExpenses", mapOf("houseId" to houseId))
        return try {
            val expenses = supabase.from("one_time_expenses")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<OneTimeExpense>()
            FlockrLogger.repoSuccess(TAG, "getOneTimeExpenses", "Found ${expenses.size} expenses")
            expenses
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getOneTimeExpenses", e)
            emptyList()
        }
    }

    fun getOneTimeExpensesFlow(houseId: String): kotlinx.coroutines.flow.Flow<List<OneTimeExpense>> {
        FlockrLogger.realtimeEvent(TAG, "getOneTimeExpensesFlow", "Starting for house=$houseId")
        return kotlinx.coroutines.flow.flow {
            // Emit initial value
            val initialExpenses = getOneTimeExpenses(houseId)
            FlockrLogger.d(TAG, "getOneTimeExpensesFlow: Emitting initial ${initialExpenses.size} expenses")
            emit(initialExpenses)

            // Create and subscribe to realtime channel
            val channelId = "expenses_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Configure realtime subscription BEFORE subscribing
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "one_time_expenses"
                }
                
                FlockrLogger.realtimeEvent(TAG, "getOneTimeExpensesFlow", "Subscribing to channel $channelId")
                // Subscribe and wait for it to be ready
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getOneTimeExpensesFlow", "Successfully subscribed")

                // Now listen for changes
                changeFlow.collect { action ->
                    FlockrLogger.realtimeEvent(TAG, "getOneTimeExpensesFlow", "Received update: $action")
                    kotlinx.coroutines.delay(100)
                    val updatedExpenses = getOneTimeExpenses(houseId)
                    FlockrLogger.d(TAG, "getOneTimeExpensesFlow: Emitting ${updatedExpenses.size} expenses after update")
                    emit(updatedExpenses)
                }
            } catch (e: Exception) {
                // If realtime fails, just keep the initial value
                FlockrLogger.repoError(TAG, "getOneTimeExpensesFlow", e)
            } finally {
                try {
                    FlockrLogger.d(TAG, "getOneTimeExpensesFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getOneTimeExpensesFlow: Error removing channel", e)
                }
            }
        }
    }

    suspend fun createOneTimeExpense(
        houseId: String,
        name: String,
        amount: Double,
        date: String,
        category: String,
        notes: String?,
        splits: List<Pair<String, Double>>? = null
    ): Result<OneTimeExpense> {
        FlockrLogger.repoStart(TAG, "createOneTimeExpense", mapOf(
            "houseId" to houseId,
            "name" to name,
            "amount" to amount,
            "hasSplits" to (splits != null && splits.isNotEmpty())
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "createOneTimeExpense: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val expenseInsert = OneTimeExpenseInsert(
                houseId = houseId,
                name = name,
                amount = amount,
                date = date,
                paidBy = currentUserId,
                category = category,
                notes = notes
            )

            val expense = supabase.from("one_time_expenses")
                .insert(expenseInsert) {
                    select()
                }
                .decodeSingle<OneTimeExpense>()

            FlockrLogger.d(TAG, "createOneTimeExpense: Expense created with id=${expense.id}")

            // Create splits if provided
            if (splits != null && splits.isNotEmpty()) {
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating ${splits.size} splits")
                splits.forEach { (splitUserId, amountOwed) ->
                    val split = ExpenseSplitInsert(
                        expenseId = expense.id,
                        userId = splitUserId,
                        amountOwed = amountOwed
                    )
                    supabase.from("expense_splits")
                        .insert(split)
                }

                // Create notification for house members about the split expense
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating split notification")
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "New Expense Split",
                    message = "Added a \$$amount expense for $name and split it.",
                    data = """{"id":"${expense.id}","type":"expense"}""",
                    excludeUserId = currentUserId
                )
                try {
                    supabase.postgrest.rpc(
                        function = "create_notification_for_house",
                        parameters = notificationParams
                    ).decodeAs<Unit>()
                    FlockrLogger.d(TAG, "createOneTimeExpense: Split notification created successfully")
                } catch (e: Exception) {
                    // Ignore notification errors, they shouldn't fail the expense creation
                    FlockrLogger.d(TAG, "createOneTimeExpense: Notification failed (non-critical): ${e.message}")
                }
            } else {
                // Create notification for simple expense (not split)
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating simple notification")
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "New Expense",
                    message = "Added a \$$amount expense for $name.",
                    data = """{"id":"${expense.id}","type":"expense"}""",
                    excludeUserId = currentUserId
                )
                try {
                    supabase.postgrest.rpc(
                        function = "create_notification_for_house",
                        parameters = notificationParams
                    ).decodeAs<Unit>()
                    FlockrLogger.d(TAG, "createOneTimeExpense: Simple notification created successfully")
                } catch (e: Exception) {
                    // Ignore notification errors, they shouldn't fail the expense creation
                    FlockrLogger.d(TAG, "createOneTimeExpense: Notification failed (non-critical): ${e.message}")
                }
            }

            FlockrLogger.repoSuccess(TAG, "createOneTimeExpense", "expense_id=${expense.id}")
            Result.success(expense)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createOneTimeExpense", e)
            Result.failure(e)
        }
    }

    suspend fun getUserBalances(houseId: String): List<UserBalance> {
        FlockrLogger.repoStart(TAG, "getUserBalances", mapOf("houseId" to houseId))
        return try {
            val balances = supabase.postgrest.rpc(
                "get_user_balances",
                mapOf("p_house_id" to houseId)
            ).decodeList<UserBalance>()
            FlockrLogger.repoSuccess(TAG, "getUserBalances", "Found ${balances.size} balances")
            balances
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getUserBalances", e)
            emptyList()
        }
    }

    suspend fun settleBalance(
        houseId: String,
        payeeId: String,
        amount: Double,
        description: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "settleBalance", mapOf(
            "houseId" to houseId,
            "payeeId" to payeeId,
            "amount" to amount
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "settleBalance: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            supabase.from("transactions")
                .insert(
                    buildMap {
                        put("house_id", houseId)
                        put("payer_id", currentUserId)
                        put("payee_id", payeeId)
                        put("amount", amount)
                        put("is_settlement", true)
                        description?.let { put("description", it) }
                    }
                )

            FlockrLogger.d(TAG, "settleBalance: Creating notification for payee")
            // Create notification for payee
            supabase.from("notifications")
                .insert(
                    buildMap {
                        put("user_id", payeeId)
                        put("house_id", houseId)
                        put("title", "Balance Settled")
                        put("message", "Has settled their balance with you (\$$amount).")
                        put("type", "settlement")
                        put("is_read", false)
                        put("data", """{"amount":"${amount}"}""")
                    }
                )

            FlockrLogger.repoSuccess(TAG, "settleBalance", "Settlement completed")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "settleBalance", e)
            Result.failure(e)
        }
    }

    suspend fun getMonthlySummary(houseId: String, month: String): MonthlySummary? {
        FlockrLogger.repoStart(TAG, "getMonthlySummary", mapOf("houseId" to houseId, "month" to month))
        return try {
            // Ensure month is in yyyy-MM-dd format for date type parameter
            val monthDate = if (month.length == 7) "$month-01" else month
            val result = supabase.postgrest.rpc(
                "get_monthly_summary",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to monthDate
                )
            ).decodeList<MonthlySummary>()

            val summary = result.firstOrNull()
            FlockrLogger.repoSuccess(TAG, "getMonthlySummary", "summary=$summary")
            summary
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getMonthlySummary", e)
            null
        }
    }

    suspend fun getSpendByMember(houseId: String, month: String): List<SpendByMember> {
        FlockrLogger.repoStart(TAG, "getSpendByMember", mapOf("houseId" to houseId, "month" to month))
        return try {
            // Ensure month is in yyyy-MM-dd format for date type parameter
            val monthDate = if (month.length == 7) "$month-01" else month
            val members = supabase.postgrest.rpc(
                function = "get_spend_by_member",
                parameters = buildMap {
                    put("p_house_id", houseId)
                    put("p_month", monthDate)
                }
            ).decodeList<SpendByMember>()
            FlockrLogger.repoSuccess(TAG, "getSpendByMember", "Found ${members.size} members")
            members
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getSpendByMember", e)
            emptyList()
        }
    }

    suspend fun getSpendByCategory(houseId: String, month: String): List<SpendByCategory> {
        FlockrLogger.repoStart(TAG, "getSpendByCategory", mapOf("houseId" to houseId, "month" to month))
        return try {
            // Ensure month is in yyyy-MM-dd format for date type parameter
            val monthDate = if (month.length == 7) "$month-01" else month
            val categories = supabase.postgrest.rpc(
                function = "get_spend_by_category",
                parameters = buildMap {
                    put("p_house_id", houseId)
                    put("p_month", monthDate)
                }
            ).decodeList<SpendByCategory>()
            FlockrLogger.repoSuccess(TAG, "getSpendByCategory", "Found ${categories.size} categories")
            categories
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getSpendByCategory", e)
            emptyList()
        }
    }

    suspend fun getPerDiemBillItemized(houseId: String, month: String): List<PerDiemBillItemized> {
        FlockrLogger.repoStart(TAG, "getPerDiemBillItemized", mapOf("houseId" to houseId, "month" to month))
        return try {
            // Ensure month is in yyyy-MM-dd format for date type parameter
            val monthDate = if (month.length == 7) "$month-01" else month
            val items = supabase.postgrest.rpc(
                function = "get_per_diem_bill_itemized",
                parameters = buildMap {
                    put("p_house_id", houseId)
                    put("p_month", monthDate)
                }
            ).decodeList<PerDiemBillItemized>()
            FlockrLogger.repoSuccess(TAG, "getPerDiemBillItemized", "Found ${items.size} items")
            items
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getPerDiemBillItemized", e)
            emptyList()
        }
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): List<PerDiemBillByMember> {
        FlockrLogger.repoStart(TAG, "getPerDiemBillByMember", mapOf("houseId" to houseId, "month" to month))
        return try {
            // Ensure month is in yyyy-MM-dd format for date type parameter
            val monthDate = if (month.length == 7) "$month-01" else month
            val members = supabase.postgrest.rpc(
                function = "get_per_diem_bill_by_member",
                parameters = buildMap {
                    put("p_house_id", houseId)
                    put("p_month", monthDate)
                }
            ).decodeList<PerDiemBillByMember>()
            FlockrLogger.repoSuccess(TAG, "getPerDiemBillByMember", "Found ${members.size} members")
            members
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getPerDiemBillByMember", e)
            emptyList()
        }
    }

    suspend fun createPerDiemEntry(
        configId: String,
        houseId: String,
        quantity: Double,
        date: String,
        notes: String?,
        itemName: String
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "createPerDiemEntry", mapOf(
            "configId" to configId,
            "houseId" to houseId,
            "quantity" to quantity,
            "itemName" to itemName
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "createPerDiemEntry: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val entryInsert = PerDiemEntryInsert(
                configId = configId,
                quantity = quantity,
                date = date,
                addedBy = currentUserId,
                notes = notes
            )
            supabase.from("per_diem_entries")
                .insert(entryInsert)

            FlockrLogger.d(TAG, "createPerDiemEntry: Creating notification")
            // Create notification
            try {
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "Per Diem Entry Added",
                    message = "$itemName entry added: $quantity received.",
                    data = """{"type":"per_diem"}""",
                    excludeUserId = currentUserId
                )
                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = notificationParams
                )
            } catch (notificationError: Exception) {
                FlockrLogger.e(TAG, "createPerDiemEntry: Failed to create notification (non-critical)", notificationError)
                // Continue - notification failure shouldn't fail the entry creation
            }

            FlockrLogger.repoSuccess(TAG, "createPerDiemEntry", "Entry created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createPerDiemEntry", e)
            Result.failure(e)
        }
    }
}

