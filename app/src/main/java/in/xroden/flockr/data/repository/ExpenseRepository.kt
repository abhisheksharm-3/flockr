package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.*
import `in`.xroden.flockr.util.FlockrLogger
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

            val insertData = buildMap<String, Any> {
                put("house_id", houseId)
                put("name", name)
                put("amount", amount)
                put("date", date)
                put("paid_by", currentUserId)
                put("category", category)
                notes?.let { put("notes", it) }
            }

            val expense = supabase.from("one_time_expenses")
                .insert(insertData) {
                    select()
                }
                .decodeSingle<OneTimeExpense>()

            FlockrLogger.d(TAG, "createOneTimeExpense: Expense created with id=${expense.id}")

            // Create splits if provided
            if (splits != null && splits.isNotEmpty()) {
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating ${splits.size} splits")
                splits.forEach { (splitUserId, amountOwed) ->
                    supabase.from("expense_splits")
                        .insert(
                            mapOf(
                                "expense_id" to expense.id,
                                "user_id" to splitUserId,
                                "amount_owed" to amountOwed
                            )
                        )
                }

                // Create notification for house members about the split expense
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating split notification")
                supabase.postgrest.rpc(
                    "create_notification_for_house",
                    mapOf(
                        "p_house_id" to houseId,
                        "p_title" to "New Expense Split",
                        "p_message" to "Added a \$$amount expense for $name and split it.",
                        "p_type" to "expense",
                        "p_data" to mapOf("id" to expense.id),
                        "p_exclude_user_id" to currentUserId
                    )
                )
            } else {
                // Create notification for simple expense (not split)
                FlockrLogger.d(TAG, "createOneTimeExpense: Creating simple notification")
                supabase.postgrest.rpc(
                    "create_notification_for_house",
                    mapOf(
                        "p_house_id" to houseId,
                        "p_title" to "New Expense",
                        "p_message" to "Added a \$$amount expense for $name.",
                        "p_type" to "expense",
                        "p_data" to mapOf("id" to expense.id),
                        "p_exclude_user_id" to currentUserId
                    )
                )
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
                    mapOf(
                        "house_id" to houseId,
                        "payer_id" to currentUserId,
                        "payee_id" to payeeId,
                        "amount" to amount,
                        "is_settlement" to true,
                        "description" to description
                    )
                )

            FlockrLogger.d(TAG, "settleBalance: Creating notification for payee")
            // Create notification for payee
            supabase.from("notifications")
                .insert(
                    mapOf(
                        "user_id" to payeeId,
                        "house_id" to houseId,
                        "title" to "Balance Settled",
                        "message" to "Has settled their balance with you (\$$amount).",
                        "type" to "settlement",
                        "is_read" to false,
                        "data" to mapOf("amount" to amount.toString())
                    )
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
            val result = supabase.postgrest.rpc(
                "get_monthly_summary",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
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
            val members = supabase.postgrest.rpc(
                "get_spend_by_member",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
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
            val categories = supabase.postgrest.rpc(
                "get_spend_by_category",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
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
            val items = supabase.postgrest.rpc(
                "get_per_diem_bill_itemized",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
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
            val members = supabase.postgrest.rpc(
                "get_per_diem_bill_by_member",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
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

            supabase.from("per_diem_entries")
                .insert(
                    mapOf(
                        "config_id" to configId,
                        "quantity" to quantity,
                        "date" to date,
                        "added_by" to currentUserId,
                        "notes" to notes
                    )
                )

            FlockrLogger.d(TAG, "createPerDiemEntry: Creating notification")
            // Create notification
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Per Diem Entry Added",
                    "p_message" to "$itemName entry added: $quantity received.",
                    "p_type" to "per_diem",
                    "p_data" to emptyMap<String, String>(),
                    "p_exclude_user_id" to currentUserId
                )
            )

            FlockrLogger.repoSuccess(TAG, "createPerDiemEntry", "Entry created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createPerDiemEntry", e)
            Result.failure(e)
        }
    }
}

