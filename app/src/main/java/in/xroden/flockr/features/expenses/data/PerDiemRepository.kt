package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.data.dto.PerDiemConfigInsert
import `in`.xroden.flockr.data.dto.PerDiemConfigUpdate
import `in`.xroden.flockr.data.dto.PerDiemEntryInsert
import `in`.xroden.flockr.data.dto.notification.HouseNotificationParams
import `in`.xroden.flockr.data.dto.perdiem.GetPerDiemBillParams
import `in`.xroden.flockr.data.dto.perdiem.PerDiemBillByMemberParams
import `in`.xroden.flockr.data.dto.perdiem.PerDiemEntriesParams
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntry
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerDiemRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IPerDiemRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override suspend fun getPerDiemConfigs(houseId: String): Result<List<PerDiemConfig>> = runCatching {
        supabase.from("per_diem_config")
            .select(Columns.ALL) {
                filter {
                    eq("house_id", houseId)
                    eq("is_active", true)
                }
            }
            .decodeList<PerDiemConfig>()
    }

    override suspend fun getPerDiemEntries(houseId: String, configId: String?): Result<List<PerDiemEntry>> = runCatching {
        supabase.from("per_diem_entries")
            .select(Columns.ALL) {
                filter { if (configId != null) eq("config_id", configId) }
                order("date", Order.DESCENDING)
                limit(count = 200)
            }
            .decodeList<PerDiemEntry>()
    }

    override suspend fun createPerDiemConfig(
        houseId: String,
        itemName: String,
        rate: BigDecimal,
        category: String,
        unit: String
    ): Result<PerDiemConfig> = runCatching {
        supabase.from("per_diem_config")
            .insert(PerDiemConfigInsert(
                houseId = houseId,
                itemName = itemName,
                rate = rate,
                category = category,
                unit = unit
            )) { select() }
            .decodeSingle<PerDiemConfig>()
    }

    override suspend fun updatePerDiemConfig(
        configId: String,
        itemName: String?,
        rate: BigDecimal?,
        category: String?,
        unit: String?
    ): Result<Unit> = runCatching {
        supabase.from("per_diem_config")
            .update(PerDiemConfigUpdate(itemName = itemName, rate = rate, category = category, unit = unit)) {
                filter { eq("id", configId) }
            }
    }

    override suspend fun deletePerDiemConfig(configId: String, deleteUsage: Boolean): Result<Unit> = runCatching {
        if (deleteUsage) {
            supabase.from("per_diem_entries").delete { filter { eq("config_id", configId) } }
        }
        supabase.from("per_diem_config")
            .update(PerDiemConfigUpdate(isActive = false)) { filter { eq("id", configId) } }
    }

    override suspend fun addPerDiemEntry(
        houseId: String,
        configId: String,
        quantity: BigDecimal,
        date: LocalDate,
        itemName: String,
        notes: String?
    ): Result<PerDiemEntry> = runCatching {
        val currentUserId = requireAuthenticated(userId)

        val entry = supabase.from("per_diem_entries")
            .insert(PerDiemEntryInsert(
                configId = configId,
                quantity = quantity,
                date = date,
                addedBy = currentUserId,
                notes = notes
            )) { select() }
            .decodeSingle<PerDiemEntry>()

        runCatching {
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

    override suspend fun deletePerDiemEntry(entryId: String): Result<Unit> = runCatching {
        supabase.from("per_diem_entries").delete { filter { eq("id", entryId) } }
    }

    override suspend fun updatePerDiemEntry(
        entryId: String,
        quantity: BigDecimal?,
        date: LocalDate?,
        notes: String?
    ): Result<Unit> = runCatching {
        supabase.from("per_diem_entries")
            .update(`in`.xroden.flockr.data.dto.PerDiemEntryUpdate(quantity = quantity, date = date, notes = notes)) {
                filter { eq("id", entryId) }
            }
    }

    override suspend fun getPerDiemBill(houseId: String, month: String): Result<List<PerDiemBillItemized>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_per_diem_bill_itemized",
            parameters = GetPerDiemBillParams(houseId = houseId, month = month)
        ).decodeAs<List<PerDiemBillItemized>>()
    }

    override suspend fun getPerDiemBillByMember(houseId: String, month: String): Result<List<PerDiemBillByMember>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_per_diem_bill_by_member",
            parameters = PerDiemBillByMemberParams(houseId = houseId, month = month)
        ).decodeAs<List<PerDiemBillByMember>>()
    }

    override suspend fun getPerDiemEntriesWithDetails(
        houseId: String,
        month: LocalDate?
    ): Result<List<PerDiemEntryWithDetails>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_per_diem_entries_with_details",
            parameters = PerDiemEntriesParams(houseId = houseId, month = month)
        ).decodeAs<List<PerDiemEntryWithDetails>>()
    }
}
