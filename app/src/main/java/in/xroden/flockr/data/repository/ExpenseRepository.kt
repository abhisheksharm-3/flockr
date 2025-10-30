package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun getOneTimeExpenses(houseId: String): List<OneTimeExpense> {
        return try {
            supabase.from("one_time_expenses")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<OneTimeExpense>()
        } catch (e: Exception) {
            emptyList()
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
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val expense = supabase.from("one_time_expenses")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "name" to name,
                        "amount" to amount,
                        "date" to date,
                        "paid_by" to currentUserId,
                        "category" to category,
                        "notes" to notes
                    )
                ) {
                    select()
                }
                .decodeSingle<OneTimeExpense>()

            // Create splits if provided
            if (splits != null && splits.isNotEmpty()) {
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
                supabase.postgrest.rpc(
                    "create_notification_for_house",
                    mapOf(
                        "p_house_id" to houseId,
                        "p_title" to "New Expense Split",
                        "p_message" to "Added a \$$amount expense for $name and split it.",
                        "p_type" to "expense",
                        "p_data" to mapOf("type" to "expense", "id" to expense.id),
                        "p_exclude_user_id" to currentUserId
                    )
                )
            } else {
                // Create notification for simple expense (not split)
                supabase.postgrest.rpc(
                    "create_notification_for_house",
                    mapOf(
                        "p_house_id" to houseId,
                        "p_title" to "New Expense",
                        "p_message" to "Added a \$$amount expense for $name.",
                        "p_type" to "expense",
                        "p_data" to mapOf("type" to "expense", "id" to expense.id),
                        "p_exclude_user_id" to currentUserId
                    )
                )
            }

            Result.success(expense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserBalances(houseId: String): List<UserBalance> {
        return try {
            supabase.postgrest.rpc(
                "get_user_balances",
                mapOf("p_house_id" to houseId)
            ).decodeList<UserBalance>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun settleBalance(
        houseId: String,
        payeeId: String,
        amount: Double,
        description: String?
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

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
                        "data" to mapOf("type" to "settlement", "amount" to amount.toString())
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlySummary(houseId: String, month: String): MonthlySummary? {
        return try {
            val result = supabase.postgrest.rpc(
                "get_monthly_summary",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeList<MonthlySummary>()

            result.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSpendByMember(houseId: String, month: String): List<SpendByMember> {
        return try {
            supabase.postgrest.rpc(
                "get_spend_by_member",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeList<SpendByMember>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSpendByCategory(houseId: String, month: String): List<SpendByCategory> {
        return try {
            supabase.postgrest.rpc(
                "get_spend_by_category",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeList<SpendByCategory>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPerDiemBillItemized(houseId: String, month: String): List<PerDiemBillItemized> {
        return try {
            supabase.postgrest.rpc(
                "get_per_diem_bill_itemized",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeList<PerDiemBillItemized>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): List<PerDiemBillByMember> {
        return try {
            supabase.postgrest.rpc(
                "get_per_diem_bill_by_member",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeList<PerDiemBillByMember>()
        } catch (e: Exception) {
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
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

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

            // Create notification
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Per Diem Entry Added",
                    "p_message" to "$itemName entry added: $quantity received.",
                    "p_data" to mapOf("type" to "per_diem"),
                    "p_exclude_user_id" to currentUserId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

