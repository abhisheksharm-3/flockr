package `in`.xroden.flockr.features.expenses.data

import io.github.jan.supabase.postgrest.postgrest

import `in`.xroden.flockr.data.dto.PerDiemConfigInsert
import `in`.xroden.flockr.data.dto.PerDiemConfigUpdate
import `in`.xroden.flockr.data.dto.PerDiemEntryInsert
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntry
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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

    suspend fun getPerDiemConfigs(houseId: String): Result<List<PerDiemConfig>> = runCatching {
        supabase.from("per_diem_config")
            .select(Columns.ALL) {
                filter {
                    eq("house_id", houseId)
                    eq("is_active", true)
                }
            }
            .decodeList<PerDiemConfig>()
    }

    suspend fun getPerDiemEntries(houseId: String, configId: String? = null): Result<List<PerDiemEntry>> = runCatching {
        supabase.from("per_diem_entries")
            .select(Columns.ALL) {
                filter {
                    if (configId != null) {
                        eq("config_id", configId)
                    }
                }
                order("date", Order.DESCENDING)
            }
            .decodeList<PerDiemEntry>()
    }

    suspend fun createPerDiemConfig(
        houseId: String,
        itemName: String,
        rate: BigDecimal,
        category: String,
        unit: String
    ): Result<PerDiemConfig> = runCatching {
        supabase.from("per_diem_config")
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
    }

    suspend fun updatePerDiemConfig(
        configId: String,
        itemName: String?,
        rate: BigDecimal?,
        category: String?,
        unit: String?
    ): Result<Unit> = runCatching {
        supabase.from("per_diem_config")
            .update(
                PerDiemConfigUpdate(
                    itemName = itemName,
                    rate = rate,
                    category = category,
                    unit = unit
                )
            ) {
                filter { eq("id", configId) }
            }
    }

    suspend fun deletePerDiemConfig(configId: String, deleteUsage: Boolean = false): Result<Unit> = runCatching {
        if (deleteUsage) {
            supabase.from("per_diem_entries")
                .delete {
                    filter { eq("config_id", configId) }
                }
        }

        // Soft delete
        supabase.from("per_diem_config")
            .update(
                PerDiemConfigUpdate(isActive = false)
            ) {
                filter { eq("id", configId) }
            }
    }

    suspend fun addPerDiemEntry(
        houseId: String,
        configId: String,
        quantity: BigDecimal,
        date: LocalDate,
        itemName: String,
        notes: String? = null
    ): Result<PerDiemEntry> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

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

        // Best effort notification
        runCatching {
            @Serializable
            data class HouseNotificationParams(
                @SerialName("p_house_id") val houseId: String,
                @SerialName("p_title") val title: String,
                @SerialName("p_message") val message: String,
                @SerialName("p_type") val type: String,
                @SerialName("p_data") val data: String,
                @SerialName("p_exclude_user_id") val excludeUserId: String?
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
        }

        entry
    }

    suspend fun deletePerDiemEntry(entryId: String): Result<Unit> = runCatching {
        supabase.from("per_diem_entries")
            .delete {
                filter { eq("id", entryId) }
            }
    }

    suspend fun updatePerDiemEntry(
        entryId: String,
        quantity: BigDecimal?,
        date: LocalDate?,
        notes: String?
    ): Result<Unit> = runCatching {
        supabase.from("per_diem_entries")
            .update(
                `in`.xroden.flockr.data.dto.PerDiemEntryUpdate(
                    quantity = quantity,
                    date = date,
                    notes = notes
                )
            ) {
                filter { eq("id", entryId) }
            }
    }

    suspend fun getPerDiemBill(houseId: String, month: String): Result<List<PerDiemBillItemized>> = runCatching {
        @Serializable
        data class PerDiemBillParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month") val month: String
        )

        supabase.postgrest.rpc(
            function = "get_per_diem_bill_itemized",
            parameters = PerDiemBillParams(
                houseId = houseId,
                month = month
            )
        ).decodeAs<List<PerDiemBillItemized>>()
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: String): Result<List<PerDiemBillByMember>> = runCatching {
        @Serializable
        data class PerDiemBillByMemberParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month") val month: String
        )

        supabase.postgrest.rpc(
            function = "get_per_diem_bill_by_member",
            parameters = PerDiemBillByMemberParams(
                houseId = houseId,
                month = month
            )
        ).decodeAs<List<PerDiemBillByMember>>()
    }

    suspend fun getPerDiemEntriesWithDetails(
        houseId: String,
        month: LocalDate? = null
    ): Result<List<PerDiemEntryWithDetails>> = runCatching {
        @Serializable
        data class PerDiemEntriesParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_month")
            @Serializable(with = `in`.xroden.flockr.data.serialization.LocalDateSerializer::class)
            val month: LocalDate? = null
        )

        supabase.postgrest.rpc(
            function = "get_per_diem_entries_with_details",
            parameters = PerDiemEntriesParams(
                houseId = houseId,
                month = month
            )
        ).decodeAs<List<PerDiemEntryWithDetails>>()
    }
}
