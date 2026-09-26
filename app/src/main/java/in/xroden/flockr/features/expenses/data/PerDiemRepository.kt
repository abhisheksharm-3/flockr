/** Per-diem items and the usage housemates record against them. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.features.expenses.data.PerDiemConfigInsert
import `in`.xroden.flockr.features.expenses.data.PerDiemConfigUpdate
import `in`.xroden.flockr.features.expenses.data.PerDiemEntryInsert
import `in`.xroden.flockr.features.expenses.data.PerDiemEntryUpdate
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.realtime.liveQuery
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.features.expenses.model.PerDiemMonth
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The monthly reads take [month] as any day in the month. */
@Singleton
class PerDiemRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    /**
     * [month]'s items, usage and billing, kept current. Entries carry no house column to filter on,
     * so their feed relies on row-level security to hear only this user's houses.
     */
    fun getMonthFlow(houseId: String, month: LocalDate): Flow<Result<PerDiemMonth>> {
        val watches = listOf(TableWatch("per_diem_config", "house_id", houseId), TableWatch("per_diem_entries"), TableWatch("expenses", "house_id", houseId))
        return supabase.liveQuery(watches) {
            coroutineScope {
                val items = async { getPerDiemConfigs(houseId).getOrThrow() }
                val entries = async { getPerDiemEntries(houseId, month).getOrThrow() }
                val byMember = async { getPerDiemBillByMember(houseId, month).getOrThrow() }
                val bill = async { getUsageBill(houseId, month).getOrThrow() }
                PerDiemMonth(items.await(), entries.await(), byMember.await(), bill.await())
            }
        }
    }

    /** The house's items still taking entries, by name. */
    suspend fun getPerDiemConfigs(houseId: String): Result<List<PerDiemConfig>> = runCatching {
        supabase.from("per_diem_config").select {
            filter {
                eq("house_id", houseId)
                eq("is_active", true)
            }
            order("item_name", Order.ASCENDING)
        }.decodeList<PerDiemConfig>()
    }

    suspend fun createPerDiemConfig(houseId: String, itemName: String, rate: BigDecimal, category: String, unit: String): Result<PerDiemConfig> =
        runCatching {
            val insert = PerDiemConfigInsert(houseId, InputSanitizer.sanitizeText(itemName), rate, category, InputSanitizer.sanitizeText(unit))
            supabase.from("per_diem_config").insert(insert) { select() }.decodeSingle<PerDiemConfig>()
        }

    /** A new [rate] applies to entries recorded from now on; entries already recorded keep their price. */
    suspend fun updatePerDiemConfig(configId: String, itemName: String?, rate: BigDecimal?, category: String?, unit: String?): Result<Unit> =
        runCatching {
            val update = PerDiemConfigUpdate(itemName?.let(InputSanitizer::sanitizeText), rate, category, unit?.let(InputSanitizer::sanitizeText))
            supabase.from("per_diem_config").update(update) { filter { eq("id", configId) } }
        }

    /**
     * Removes an item from use. By default it is archived, so the months it was used in still bill
     * it; with [deleteUsage] it is deleted with every entry recorded against it, which only an admin may do.
     */
    suspend fun deletePerDiemConfig(configId: String, deleteUsage: Boolean): Result<Unit> = runCatching {
        if (deleteUsage) {
            val deleted = supabase.from("per_diem_config").delete {
                filter { eq("id", configId) }
                select()
            }.decodeList<PerDiemConfig>()
            if (deleted.isEmpty()) throw DomainError.ValidationError.Rule("Only an admin can delete an item and its usage")
        } else {
            supabase.from("per_diem_config").update(PerDiemConfigUpdate(isActive = false)) { filter { eq("id", configId) } }
        }
    }

    suspend fun addPerDiemEntry(configId: String, quantity: BigDecimal, date: LocalDate, notes: String?): Result<Unit> = runCatching {
        val userId = requireAuthenticated(supabase.auth.currentUserOrNull()?.id)
        supabase.from("per_diem_entries").insert(PerDiemEntryInsert(configId, quantity, date, userId, notes?.let(InputSanitizer::sanitizeText)))
    }

    suspend fun updatePerDiemEntry(entryId: String, quantity: BigDecimal?, date: LocalDate?, notes: String?): Result<Unit> = runCatching {
        supabase.from("per_diem_entries").update(PerDiemEntryUpdate(quantity, date, notes?.let(InputSanitizer::sanitizeText))) {
            filter { eq("id", entryId) }
        }
    }

    suspend fun deletePerDiemEntry(entryId: String): Result<Unit> = runCatching {
        supabase.from("per_diem_entries").delete { filter { eq("id", entryId) } }
    }

    suspend fun getPerDiemEntries(houseId: String, month: LocalDate): Result<List<PerDiemEntryWithDetails>> = runCatching {
        supabase.postgrest.rpc("get_per_diem_entries", monthParams(houseId, month)).decodeList<PerDiemEntryWithDetails>()
    }

    suspend fun getPerDiemBill(houseId: String, month: LocalDate): Result<List<PerDiemBillItemized>> = runCatching {
        supabase.postgrest.rpc("get_per_diem_bill_itemized", monthParams(houseId, month)).decodeList<PerDiemBillItemized>()
    }

    suspend fun getPerDiemBillByMember(houseId: String, month: LocalDate): Result<List<PerDiemBillByMember>> = runCatching {
        supabase.postgrest.rpc("get_per_diem_bill_by_member", monthParams(houseId, month)).decodeList<PerDiemBillByMember>()
    }

    /**
     * Turns [month]'s usage into one expense the signed-in user paid, owed by each member as their
     * entries cost. The month's entries are then fixed until that expense is deleted.
     */
    suspend fun billMonth(houseId: String, month: LocalDate): Result<Unit> = runCatching {
        supabase.postgrest.rpc("bill_per_diem_month", monthParams(houseId, month))
    }

    /** The expense [month]'s usage was billed as, or null while it is unbilled. */
    suspend fun getUsageBill(houseId: String, month: LocalDate): Result<Expense?> = runCatching {
        supabase.from("expenses").select(EXPENSE_WITH_SHARES) {
            filter {
                eq("house_id", houseId)
                eq("per_diem_month", LocalDate(month.year, month.month, 1).toString())
            }
        }.decodeSingleOrNull<Expense>()
    }

    private fun monthParams(houseId: String, month: LocalDate): JsonObject = buildJsonObject {
        put("p_house_id", houseId)
        put("p_month", month.toString())
    }
}
