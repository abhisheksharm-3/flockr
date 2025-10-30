package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.PerDiemConfig
import `in`.xroden.flockr.data.model.PerDiemEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerDiemRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun getPerDiemConfigs(houseId: String): List<PerDiemConfig> {
        return try {
            supabase.from("per_diem_configs")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_active", true)
                    }
                }
                .decodeList<PerDiemConfig>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPerDiemEntries(houseId: String, configId: String? = null): List<PerDiemEntry> {
        return try {
            supabase.from("per_diem_entries")
                .select(Columns.ALL) {
                    filter {
                        if (configId != null) {
                            eq("config_id", configId)
                        }
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<PerDiemEntry>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPerDiemConfig(
        houseId: String,
        itemName: String,
        rate: Double,
        category: String,
        unit: String
    ): Result<PerDiemConfig> {
        return try {
            val config = supabase.from("per_diem_configs")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "item_name" to itemName,
                        "rate" to rate,
                        "category" to category,
                        "unit" to unit,
                        "is_active" to true
                    )
                ) {
                    select()
                }
                .decodeSingle<PerDiemConfig>()

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePerDiemConfig(
        configId: String,
        itemName: String,
        rate: Double,
        category: String,
        unit: String
    ): Result<Unit> {
        return try {
            supabase.from("per_diem_configs")
                .update(
                    mapOf(
                        "item_name" to itemName,
                        "rate" to rate,
                        "category" to category,
                        "unit" to unit
                    )
                ) {
                    filter {
                        eq("id", configId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePerDiemConfig(configId: String): Result<Unit> {
        return try {
            supabase.from("per_diem_configs")
                .update(
                    mapOf("is_active" to false)
                ) {
                    filter {
                        eq("id", configId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPerDiemEntry(
        houseId: String,
        configId: String,
        quantity: Double,
        date: String,
        itemName: String
    ): Result<PerDiemEntry> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val entry = supabase.from("per_diem_entries")
                .insert(
                    mapOf(
                        "config_id" to configId,
                        "quantity" to quantity,
                        "date" to date,
                        "added_by" to currentUserId
                    )
                ) {
                    select()
                }
                .decodeSingle<PerDiemEntry>()

            // Create notification for house members
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Per-Diem Entry Added",
                    "p_message" to "$itemName entry added: $quantity units.",
                    "p_type" to "per_diem",
                    "p_data" to mapOf("type" to "per_diem", "id" to entry.id),
                    "p_exclude_user_id" to currentUserId
                )
            )

            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPerDiemBillItemized(houseId: String, month: String): Map<String, Any>? {
        return try {
            supabase.postgrest.rpc(
                "get_per_diem_bill_itemized",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeSingle<Map<String, Any>>() // <-- FIXED: Added .decodeSingle()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): Map<String, Any>? {
        return try {
            supabase.postgrest.rpc(
                "get_per_diem_bill_by_member",
                mapOf(
                    "p_house_id" to houseId,
                    "p_month" to month
                )
            ).decodeSingle<Map<String, Any>>() // <-- FIXED: Added .decodeSingle()
        } catch (e: Exception) {
            null
        }
    }
}