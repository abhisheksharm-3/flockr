package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.PerDiemConfigInsert
import `in`.xroden.flockr.data.dto.PerDiemConfigUpdate
import `in`.xroden.flockr.data.dto.PerDiemEntryInsert
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntry
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerDiemRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun getPerDiemConfigs(houseId: String): Result<List<PerDiemConfig>> {
        return try {
            val configs = supabase.from("per_diem_config")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_active", true)
                    }
                }
                .decodeList<PerDiemConfig>()

            Result.success(configs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPerDiemEntries(houseId: String, configId: String? = null): Result<List<PerDiemEntry>> {
        return try {
            val entries = supabase.from("per_diem_entries")
                .select(Columns.ALL) {
                    filter {
                        if (configId != null) {
                            eq("config_id", configId)
                        }
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<PerDiemEntry>()

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPerDiemConfig(
        houseId: String,
        itemName: String,
        rate: BigDecimal,
        category: String,
        unit: String
    ): Result<PerDiemConfig> {
        return try {
            val config = supabase.from("per_diem_config")
                .insert(
                    PerDiemConfigInsert(
                        houseId = houseId,
                        itemName = itemName,
                        rate = rate,
                        category = category,
                        unit = unit
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
        itemName: String?,
        rate: BigDecimal?,
        category: String?,
        unit: String?
    ): Result<Unit> {
        return try {
            supabase.from("per_diem_config")
                .update(
                    PerDiemConfigUpdate(
                        itemName = itemName,
                        rate = rate,
                        category = category,
                        unit = unit
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

    suspend fun deletePerDiemConfig(configId: String, deleteUsage: Boolean = false): Result<Unit> {
        return try {
            if (deleteUsage) {
                supabase.from("per_diem_entries")
                    .delete {
                        filter {
                            eq("config_id", configId)
                        }
                    }
            }

            // Soft delete by setting isActive to false
            supabase.from("per_diem_config")
                .update(
                    PerDiemConfigUpdate(isActive = false)
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
        quantity: BigDecimal,
        date: LocalDate,
        itemName: String,
        notes: String? = null
    ): Result<PerDiemEntry> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val entry = supabase.from("per_diem_entries")
                .insert(
                    PerDiemEntryInsert(
                        configId = configId,
                        quantity = quantity,
                        date = date,
                        addedBy = currentUserId,
                        notes = notes
                    )
                ) {
                    select()
                }
                .decodeSingle<PerDiemEntry>()

            // Create notification
            try {
                @Serializable
                data class HouseNotificationParams(
                    @SerialName("p_house_id")
                    val houseId: String,
                    @SerialName("p_title")
                    val title: String,
                    @SerialName("p_message")
                    val message: String,
                    @SerialName("p_type")
                    val type: String,
                    @SerialName("p_data")
                    val data: String,
                    @SerialName("p_exclude_user_id")
                    val excludeUserId: String?
                )

                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = HouseNotificationParams(
                        houseId = houseId,
                        title = "Per-Diem Entry Added",
                        message = "Added $itemName entry: $quantity units.",
                        type = "per_diem",
                        data = """{"id":"${entry.id}","type":"per_diem","item":"$itemName"}""",
                        excludeUserId = currentUserId
                    )
                )
            } catch (e: Exception) {
                // Ignore notification errors
            }

            Result.success(entry)
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

    suspend fun updatePerDiemEntry(
        entryId: String,
        quantity: BigDecimal?,
        date: LocalDate?,
        notes: String?
    ): Result<Unit> {
        return try {
            supabase.from("per_diem_entries")
                .update(
                    `in`.xroden.flockr.data.dto.PerDiemEntryUpdate(
                        quantity = quantity,
                        date = date,
                        notes = notes
                    )
                ) {
                    filter {
                        eq("id", entryId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPerDiemBill(houseId: String, month: String): Result<List<PerDiemBillItemized>> {
        return try {
            @Serializable
            data class PerDiemBillParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                val month: String
            )

            android.util.Log.d("PerDiemRepository", "getPerDiemBill called for houseId: $houseId, month: $month")
            val bill = supabase.postgrest.rpc(
                function = "get_per_diem_bill_itemized",
                parameters = PerDiemBillParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeAs<List<PerDiemBillItemized>>()
            android.util.Log.d("PerDiemRepository", "getPerDiemBill result: ${bill.size} items")
            Result.success(bill)
        } catch (e: Exception) {
            android.util.Log.e("PerDiemRepository", "getPerDiemBill failed", e)
            Result.failure(e)
        }
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): Result<List<PerDiemBillByMember>> {
        return try {
            @Serializable
            data class PerDiemBillByMemberParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                val month: String
            )

            android.util.Log.d("PerDiemRepository", "getPerDiemBillByMember called for houseId: $houseId, month: $month")
            val bill = supabase.postgrest.rpc(
                function = "get_per_diem_bill_by_member",
                parameters = PerDiemBillByMemberParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeAs<List<PerDiemBillByMember>>()
            android.util.Log.d("PerDiemRepository", "getPerDiemBillByMember result: ${bill.size} items")
            Result.success(bill)
        } catch (e: Exception) {
            android.util.Log.e("PerDiemRepository", "getPerDiemBillByMember failed", e)
            Result.failure(e)
        }
    }

    suspend fun getPerDiemEntriesWithDetails(
        houseId: String,
        month: LocalDate? = null
    ): Result<List<PerDiemEntryWithDetails>> {
        return try {
            @Serializable
            data class PerDiemEntriesParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_month")
                @Serializable(with = `in`.xroden.flockr.data.serialization.LocalDateSerializer::class)
                val month: LocalDate? = null
            )

            val entries = supabase.postgrest.rpc(
                function = "get_per_diem_entries_with_details",
                parameters = PerDiemEntriesParams(
                    houseId = houseId,
                    month = month
                )
            ).decodeAs<List<PerDiemEntryWithDetails>>()

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
