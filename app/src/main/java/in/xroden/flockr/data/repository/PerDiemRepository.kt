package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.CreateNotificationParams
import `in`.xroden.flockr.data.model.GetPerDiemBillByMonthParams
import `in`.xroden.flockr.data.model.GetPerDiemBillParams
import `in`.xroden.flockr.data.model.PerDiemConfig
import `in`.xroden.flockr.data.model.PerDiemConfigActivation
import `in`.xroden.flockr.data.model.PerDiemConfigInsert
import `in`.xroden.flockr.data.model.PerDiemConfigUpdate
import `in`.xroden.flockr.data.model.PerDiemEntry
import `in`.xroden.flockr.data.model.PerDiemEntryInsert
import `in`.xroden.flockr.data.model.PerDiemEntryWithDetails
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
            android.util.Log.d("PerDiemRepository", "Getting per-diem configs for house: $houseId")
            val configs = supabase.from("per_diem_config")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_active", true)
                    }
                }
                .decodeList<PerDiemConfig>()
            android.util.Log.d("PerDiemRepository", "Found ${configs.size} per-diem configs")
            configs
        } catch (e: Exception) {
            android.util.Log.e("PerDiemRepository", "Error getting per-diem configs", e)
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
            android.util.Log.d("PerDiemRepository", "Creating per-diem config: $itemName")
            val configInsert = PerDiemConfigInsert(
                houseId = houseId,
                itemName = itemName,
                rate = rate,
                category = category,
                unit = unit,
                isActive = true
            )
            val config = supabase.from("per_diem_config")
                .insert(configInsert) {
                    select()
                }
                .decodeSingle<PerDiemConfig>()

            android.util.Log.d("PerDiemRepository", "Per-diem config created with id: ${config.id}")
            Result.success(config)
        } catch (e: Exception) {
            android.util.Log.e("PerDiemRepository", "Error creating per-diem config", e)
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
            val update = PerDiemConfigUpdate(
                itemName = itemName,
                rate = rate,
                category = category,
                unit = unit
            )
            supabase.from("per_diem_config")
                .update(update) {
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
            val activation = PerDiemConfigActivation(isActive = false)
            supabase.from("per_diem_config")
                .update(activation) {
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

            val entryInsert = PerDiemEntryInsert(
                configId = configId,
                quantity = quantity,
                date = date,
                addedBy = currentUserId,
                notes = null
            )
            val entry = supabase.from("per_diem_entries")
                .insert(entryInsert) {
                    select()
                }
                .decodeSingle<PerDiemEntry>()

            // Create notification for house members
            val notificationParams = CreateNotificationParams(
                houseId = houseId,
                title = "Per-Diem Entry Added",
                message = "$itemName entry added: $quantity units.",
                data = """{"id":"${entry.id}","type":"per_diem"}""",
                excludeUserId = currentUserId
            )
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = notificationParams
            ).decodeAs<Unit>()

            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePerDiemEntry(
        entryId: String,
        quantity: Double,
        date: String,
        notes: String?
    ): Result<Unit> {
        return try {
            val update = mapOf(
                "quantity" to quantity,
                "date" to date,
                "notes" to notes
            )
            supabase.from("per_diem_entries")
                .update(update) {
                    filter {
                        eq("id", entryId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePerDiemEntry(entryId: String): Result<Unit> {
        return try {
            supabase.from("per_diem_entries")
                .delete {
                    filter {
                        eq("id", entryId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPerDiemEntriesWithDetails(houseId: String, month: String? = null): List<PerDiemEntryWithDetails> {
        return try {
            val monthDate = if (month != null && month.length == 7) "$month-01" else month
            supabase.postgrest.rpc(
                function = "get_per_diem_entries_with_details",
                parameters = buildMap {
                    put("p_house_id", houseId)
                    if (monthDate != null) {
                        put("p_month", monthDate)
                    }
                }
            ).decodeList<PerDiemEntryWithDetails>()
        } catch (e: Exception) {
            android.util.Log.e("PerDiemRepository", "Error getting per diem entries with details", e)
            emptyList()
        }
    }

    suspend fun getPerDiemBillItemized(houseId: String, month: String): Map<String, Any>? {
        return try {
            val params = GetPerDiemBillByMonthParams(
                houseId = houseId,
                month = month
            )
            supabase.postgrest.rpc(
                function = "get_per_diem_bill_itemized",
                parameters = params
            ).decodeSingle<Map<String, Any>>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): Map<String, Any>? {
        return try {
            val params = GetPerDiemBillByMonthParams(
                houseId = houseId,
                month = month
            )
            supabase.postgrest.rpc(
                function = "get_per_diem_bill_by_member",
                parameters = params
            ).decodeSingle<Map<String, Any>>()
        } catch (e: Exception) {
            null
        }
    }
}