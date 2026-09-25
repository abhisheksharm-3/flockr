/** A house's activity feed, as the database records it. */
package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.features.house.model.HouseAuditLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

private const val ACTIVITY_PAGE = 200L

@Singleton
class HouseAuditRepository @Inject constructor(private val supabase: SupabaseClient) {

    /** The house's latest activity, newest first. */
    suspend fun getActivity(houseId: String): Result<List<HouseAuditLog>> = runCatching {
        supabase.from("house_audit_log").select {
            filter { eq("house_id", houseId) }
            order("created_at", Order.DESCENDING)
            limit(ACTIVITY_PAGE)
        }.decodeList<HouseAuditLog>()
    }
}
