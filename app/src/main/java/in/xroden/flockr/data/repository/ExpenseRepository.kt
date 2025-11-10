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

    /**
     * Helper function to get house currency symbol for notifications
     */
    private suspend fun getHouseCurrency(houseId: String): String {
        return try {
            val result = supabase.from("house_config")
                .select(columns = Columns.list("currency_symbol")) {
                    filter { eq("house_id", houseId) }
                }
                .decodeSingle<Map<String, String>>()
            result["currency_symbol"] ?: "$"
        } catch (e: Exception) {
            FlockrLogger.d(TAG, "Failed to get house currency, using default: ${e.message}")
            "$" // Fallback to USD
        }
    }

    /**
     * Helper function to get current user's display name for notifications
     */
    private suspend fun getCurrentUserName(): String {
        val currentUserId = userId ?: return "Someone"
        return try {
            val profile = supabase.from("profiles")
                .select(Columns.raw("full_name")) {
                    filter { eq("user_id", currentUserId) }
                }
                .decodeSingle<Profile>()
            profile.fullName ?: "Someone"
        } catch (e: Exception) {
            FlockrLogger.d(TAG, "Failed to get user name, using default: ${e.message}")
            "Someone"
        }
    }

    /**
     * Delete a one-time expense and its splits
     */
    suspend fun deleteOneTimeExpense(expenseId: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "deleteOneTimeExpense", mapOf("expenseId" to expenseId))
        return try {
            // Delete splits first (foreign key constraint)
            supabase.from("expense_splits")
                .delete {
                    filter { eq("expense_id", expenseId) }
                }

            // Delete the expense
            supabase.from("one_time_expenses")
                .delete {
                    filter { eq("id", expenseId) }
                }

            FlockrLogger.repoSuccess(TAG, "deleteOneTimeExpense", "Expense deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "deleteOneTimeExpense", e)
            Result.failure(e)
        }
    }

    /**
     * Update a one-time expense
     */
    suspend fun updateOneTimeExpense(
        expenseId: String,
        name: String,
        amount: Double,
        date: String,
        category: String,
        notes: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "updateOneTimeExpense", mapOf("expenseId" to expenseId))
        return try {
            val update = mapOf(
                "name" to name,
                "amount" to amount,
                "date" to date,
                "category" to category,
                "notes" to notes
            )

            supabase.from("one_time_expenses")
                .update(update) {
                    filter { eq("id", expenseId) }
                }

            FlockrLogger.repoSuccess(TAG, "updateOneTimeExpense", "Expense updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "updateOneTimeExpense", e)
            Result.failure(e)
        }
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

    /**
     * Create a one-time expense.
     *
     * Split Logic:
     * - splits = null: No splitting, just track the expense
     * - splits = list: Split expense among specified users
     *
     * IMPORTANT: If only the payer is in the splits list, splits are NOT created
     * to prevent "owing yourself" scenarios. This is filtered out automatically.
     *
     * Example Scenarios:
     * 1. Solo expense: You pay 676, no splits → No balance change
     * 2. Split expense: You pay 900, split among 3 → You're owed 600, others owe 300 each
     * 3. Full expense shown in house total regardless of splits
     */
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

            // Create splits if provided (but filter out if only payer is in the split)
            if (splits != null && splits.isNotEmpty()) {
                // Don't create splits if it's only the payer (would owe themselves)
                val validSplits = if (splits.size == 1 && splits.first().first == currentUserId) {
                    FlockrLogger.d(TAG, "createOneTimeExpense: Skipping split creation - only payer is selected")
                    emptyList()
                } else {
                    splits
                }

                if (validSplits.isNotEmpty()) {
                    FlockrLogger.d(TAG, "createOneTimeExpense: Creating ${validSplits.size} splits")
                    validSplits.forEach { (splitUserId, amountOwed) ->
                        val split = ExpenseSplitInsert(
                            expenseId = expense.id,
                            userId = splitUserId,
                            amountOwed = amountOwed
                        )
                        supabase.from("expense_splits")
                            .insert(split)
                    }
                }

                // Get currency and user name for notification
                val currency = getHouseCurrency(houseId)
                val userName = getCurrentUserName()

                // Create notification for house members about the split expense
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating split notification")
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "New Expense Split",
                    message = "$userName added a $currency$amount expense for $name and split it.",
                    type = "expense",
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
                // Get currency and user name for notification
                val currency = getHouseCurrency(houseId)
                val userName = getCurrentUserName()

                // Create notification for simple expense (not split)
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating simple notification")
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "New Expense",
                    message = "$userName added a $currency$amount expense for $name.",
                    type = "expense",
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

    /**
     * Get user balances for a house.
     *
     * Balance Calculation Logic:
     * - Positive balance: User is OWED money (they paid more than they owe)
     * - Negative balance: User OWES money (they owe more than they paid)
     *
     * IMPORTANT: If you see "you owe yourself" bugs, the RPC function needs to be updated.
     * See FIX_BALANCE_RPC.sql and BALANCE_FIX_GUIDE.md for the database fix.
     *
     * The RPC function must exclude splits where user_id == payer_id to prevent
     * self-owing scenarios.
     */
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

            // Get current user's name for notification
            val currentUserProfile = try {
                supabase.from("profiles")
                    .select(Columns.raw("full_name")) {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeSingle<Profile>()
            } catch (e: Exception) {
                null
            }
            val payerName = currentUserProfile?.fullName ?: "Someone"

            val transactionInsert = TransactionInsert(
                houseId = houseId,
                payerId = currentUserId,
                payeeId = payeeId,
                amount = amount,
                isSettlement = true,
                description = description
            )

            supabase.from("transactions")
                .insert(transactionInsert)

            // Get house currency for notification
            val currency = getHouseCurrency(houseId)

            FlockrLogger.d(TAG, "settleBalance: Creating notification for payee")
            // Create notification for payee
            val notificationInsert = NotificationInsert(
                userId = payeeId,
                houseId = houseId,
                title = "Balance Settled",
                message = "$payerName has settled their balance with you ($currency$amount).",
                type = "settlement",
                isRead = false,
                data = """{"amount":"${amount}","payer_name":"$payerName","currency":"$currency"}"""
            )

            supabase.from("notifications")
                .insert(notificationInsert)

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
            val params = GetPerDiemBillByMonthParams(houseId = houseId, month = monthDate)
            val members = supabase.postgrest.rpc(
                function = "get_spend_by_member",
                parameters = params
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
            val params = GetSpendByCategoryParams(houseId = houseId, month = monthDate)
            val categories = supabase.postgrest.rpc(
                function = "get_spend_by_category",
                parameters = params
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
            val params = GetPerDiemBillByMonthParams(houseId = houseId, month = monthDate)
            val items = supabase.postgrest.rpc(
                function = "get_per_diem_bill_itemized",
                parameters = params
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
            val params = GetPerDiemBillByMonthParams(houseId = houseId, month = monthDate)
            val members = supabase.postgrest.rpc(
                function = "get_per_diem_bill_by_member",
                parameters = params
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

            // Get currency and user name for notification
            val currency = getHouseCurrency(houseId)
            val userName = getCurrentUserName()

            FlockrLogger.d(TAG, "createPerDiemEntry: Creating notification")
            // Create notification
            try {
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "Per Diem Entry Added",
                    message = "$userName added $itemName entry: $quantity units.",
                    type = "per_diem",
                    data = """{"type":"per_diem","item":"$itemName"}""",
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

    // ========================================
    // RECURRING EXPENSES METHODS
    // ========================================

    /**
     * Get all recurring expenses for a house with due status
     */
    suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>> {
        FlockrLogger.repoStart(TAG, "getRecurringExpenses", mapOf("houseId" to houseId))
        return try {
            // Use RPC function to get expenses with calculated due status
            val expenses = supabase.postgrest.rpc(
                function = "get_recurring_expenses_with_status",
                parameters = mapOf("p_house_id" to houseId)
            ).decodeList<RecurringExpense>()

            FlockrLogger.repoSuccess(TAG, "getRecurringExpenses", "Found ${expenses.size} recurring expenses")
            Result.success(expenses)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getRecurringExpenses", e)
            Result.failure(e)
        }
    }

    /**
     * Get recurring expenses with real-time updates
     */
    fun getRecurringExpensesFlow(houseId: String): kotlinx.coroutines.flow.Flow<List<RecurringExpense>> {
        FlockrLogger.realtimeEvent(TAG, "getRecurringExpensesFlow", "Starting for house=$houseId")

        return kotlinx.coroutines.flow.flow {
            // First emit initial data
            val initialExpenses = getRecurringExpenses(houseId)
            val expenses = initialExpenses.getOrElse { emptyList() }
            FlockrLogger.d(TAG, "getRecurringExpensesFlow: Emitting initial ${expenses.size} expenses")
            emit(expenses)

            // Create and subscribe to realtime channel
            val channelId = "recurring_expenses_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Configure realtime subscription BEFORE subscribing
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "recurring_expenses"
                }

                FlockrLogger.realtimeEvent(TAG, "getRecurringExpensesFlow", "Subscribing to channel $channelId")
                // Subscribe and wait for it to be ready
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getRecurringExpensesFlow", "Successfully subscribed")

                // Listen for changes
                changeFlow.collect { change ->
                    FlockrLogger.realtimeEvent(TAG, "getRecurringExpensesFlow", "Change detected: ${change::class.simpleName}")

                    // Add a small delay to ensure database consistency
                    kotlinx.coroutines.delay(100)

                    // Reload all expenses (filtered by houseId in the query)
                    val updatedExpenses = getRecurringExpenses(houseId)
                    val newExpenses = updatedExpenses.getOrElse { emptyList() }
                    FlockrLogger.d(TAG, "getRecurringExpensesFlow: Emitting updated ${newExpenses.size} expenses")
                    emit(newExpenses)
                }
            } catch (e: Exception) {
                // If realtime fails, just keep the initial value
                FlockrLogger.repoError(TAG, "getRecurringExpensesFlow", e)
            } finally {
                try {
                    FlockrLogger.d(TAG, "getRecurringExpensesFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getRecurringExpensesFlow: Error removing channel", e)
                }
            }
        }
    }

    /**
     * Create a new recurring expense
     */
    suspend fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: Double,
        dueDay: Int,
        category: String,
        frequency: String = "monthly",
        customFrequencyDays: Int? = null,
        reminderDaysBefore: Int = 3,
        reminderEnabled: Boolean = true,
        notes: String? = null
    ): Result<RecurringExpense> {
        FlockrLogger.repoStart(TAG, "createRecurringExpense", mapOf(
            "houseId" to houseId,
            "name" to name,
            "amount" to amount,
            "frequency" to frequency
        ))

        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "createRecurringExpense: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val expenseInsert = RecurringExpenseInsert(
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
                notes = notes
            )

            val expense = supabase.from("recurring_expenses")
                .insert(expenseInsert) {
                    select()
                }
                .decodeSingle<RecurringExpense>()

            FlockrLogger.d(TAG, "createRecurringExpense: Expense created with id=${expense.id}")

            // Get currency and user name for notification
            val currency = getHouseCurrency(houseId)
            val userName = getCurrentUserName()

            // Create notification for house members
            val notificationParams = CreateNotificationParams(
                houseId = houseId,
                title = "New Recurring Bill",
                message = "$userName added $frequency bill: $name for $currency$amount.",
                type = "recurring_expense",
                data = """{"id":"${expense.id}","type":"recurring_expense"}""",
                excludeUserId = currentUserId
            )

            try {
                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = notificationParams
                ).decodeAs<Unit>()
                FlockrLogger.d(TAG, "createRecurringExpense: Notification created successfully")
            } catch (e: Exception) {
                FlockrLogger.d(TAG, "createRecurringExpense: Notification failed (non-critical): ${e.message}")
            }

            FlockrLogger.repoSuccess(TAG, "createRecurringExpense", "Expense created successfully")
            Result.success(expense)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createRecurringExpense", e)
            Result.failure(e)
        }
    }

    /**
     * Update a recurring expense
     */
    suspend fun updateRecurringExpense(
        expenseId: String,
        name: String? = null,
        amount: Double? = null,
        dueDay: Int? = null,
        category: String? = null,
        isActive: Boolean? = null,
        frequency: String? = null,
        customFrequencyDays: Int? = null,
        reminderDaysBefore: Int? = null,
        reminderEnabled: Boolean? = null,
        notes: String? = null
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "updateRecurringExpense", mapOf(
            "expenseId" to expenseId,
            "name" to name,
            "amount" to amount
        ))

        return try {
            val update = RecurringExpenseUpdate(
                name = name,
                amount = amount,
                dueDay = dueDay,
                category = category,
                isActive = isActive,
                frequency = frequency,
                customFrequencyDays = customFrequencyDays,
                reminderDaysBefore = reminderDaysBefore,
                reminderEnabled = reminderEnabled,
                notes = notes
            )

            supabase.from("recurring_expenses")
                .update(update) {
                    filter {
                        eq("id", expenseId)
                    }
                }

            FlockrLogger.repoSuccess(TAG, "updateRecurringExpense", "Expense updated")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "updateRecurringExpense", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a recurring expense
     */
    suspend fun deleteRecurringExpense(expenseId: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "deleteRecurringExpense", mapOf("expenseId" to expenseId))
        return try {
            // Delete payment history first
            supabase.from("payment_history")
                .delete {
                    filter { eq("recurring_expense_id", expenseId) }
                }

            // Delete the recurring expense
            supabase.from("recurring_expenses")
                .delete {
                    filter { eq("id", expenseId) }
                }

            FlockrLogger.repoSuccess(TAG, "deleteRecurringExpense", "Recurring expense deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "deleteRecurringExpense", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle active status of a recurring expense
     */
    suspend fun toggleRecurringExpenseActive(
        expenseId: String,
        isActive: Boolean
    ): Result<Unit> {
        return updateRecurringExpense(expenseId, isActive = isActive)
    }

    /**
     * Mark a recurring expense as paid for this period
     * This records the payment in payment_history and updates the last_paid_date
     */
    suspend fun markRecurringExpenseAsPaid(
        expenseId: String,
        houseId: String,
        amount: Double,
        paymentDate: String
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "markRecurringExpenseAsPaid", mapOf(
            "expenseId" to expenseId,
            "amount" to amount,
            "paymentDate" to paymentDate
        ))

        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "markRecurringExpenseAsPaid: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            // 1. Record payment in payment_history
            val paymentInsert = PaymentHistoryInsert(
                recurringExpenseId = expenseId,
                paidBy = currentUserId,
                amount = amount,
                paymentDate = paymentDate
            )

            supabase.from("payment_history")
                .insert(paymentInsert) {
                    select()
                }

            // 2. Update last_paid_date in recurring_expenses (trigger will calc next_due_date)
            val update = RecurringExpenseUpdate(
                lastPaidDate = paymentDate
            )

            supabase.from("recurring_expenses")
                .update(update) {
                    filter {
                        eq("id", expenseId)
                    }
                }

            FlockrLogger.d(TAG, "markRecurringExpenseAsPaid: Creating notification")

            // Get currency and user name for notification
            val currency = getHouseCurrency(houseId)
            val userName = getCurrentUserName()

            // 3. Create notification
            try {
                val notificationParams = CreateNotificationParams(
                    houseId = houseId,
                    title = "Bill Paid",
                    message = "$userName marked recurring bill as paid ($currency$amount).",
                    type = "payment",
                    data = """{"id":"$expenseId","type":"payment","amount":"$amount"}""",
                    excludeUserId = currentUserId
                )
                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = notificationParams
                ).decodeAs<Unit>()
            } catch (e: Exception) {
                FlockrLogger.d(TAG, "markRecurringExpenseAsPaid: Notification failed (non-critical)")
            }

            FlockrLogger.repoSuccess(TAG, "markRecurringExpenseAsPaid", "Payment recorded")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "markRecurringExpenseAsPaid", e)
            Result.failure(e)
        }
    }

    /**
     * Get payment history for a recurring expense
     */
    suspend fun getRecurringExpensePayments(expenseId: String): Result<List<PaymentHistory>> {
        FlockrLogger.repoStart(TAG, "getRecurringExpensePayments", mapOf("expenseId" to expenseId))

        return try {
            val payments = supabase.from("payment_history")
                .select(Columns.ALL) {
                    filter {
                        eq("recurring_expense_id", expenseId)
                    }
                    order("payment_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<PaymentHistory>()

            FlockrLogger.repoSuccess(TAG, "getRecurringExpensePayments", "Found ${payments.size} payments")
            Result.success(payments)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getRecurringExpensePayments", e)
            Result.failure(e)
        }
    }
}

