/** A house's spending for a calendar month, as the reports screen shows it. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Every report excludes payments between housemates and counts a recurring bill's payment once. [month] may be any day in it. */
@Singleton
class ExpenseAnalyticsRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getMonthlySummary(houseId: String, month: LocalDate): Result<MonthlySummary> = runCatching {
        supabase.postgrest.rpc("get_monthly_summary", monthParams(houseId, month)).decodeSingle<MonthlySummary>()
    }

    suspend fun getSpendByMember(houseId: String, month: LocalDate): Result<List<SpendByMember>> = runCatching {
        supabase.postgrest.rpc("get_spend_by_member", monthParams(houseId, month)).decodeList<SpendByMember>()
    }

    suspend fun getSpendByCategory(houseId: String, month: LocalDate): Result<List<SpendByCategory>> = runCatching {
        supabase.postgrest.rpc("get_spend_by_category", monthParams(houseId, month)).decodeList<SpendByCategory>()
    }

    private fun monthParams(houseId: String, month: LocalDate): JsonObject = buildJsonObject {
        put("p_house_id", houseId)
        put("p_month", month.toString())
    }
}
