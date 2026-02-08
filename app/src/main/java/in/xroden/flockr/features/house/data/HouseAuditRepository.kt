package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.features.house.model.HouseAuditLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling house audit log operations.
 * Provides read-only access to house activity history.
 */
@Singleton
class HouseAuditRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IHouseAuditRepository {
    /**
     * Fetches audit logs for a house, ordered by creation date descending.
     * Returns empty list on failure.
     */
    override suspend fun getHouseAuditLogs(houseId: String): List<HouseAuditLog> {
        return try {
            supabase.from("house_audit_log")
                .select(Columns.ALL) {
                    filter { eq("house_id", houseId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<HouseAuditLog>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches the most recent audit logs for a house.
     */
    override suspend fun getRecentAuditLogs(houseId: String, limit: Int): List<HouseAuditLog> {
        return try {
            supabase.from("house_audit_log")
                .select(Columns.ALL) {
                    filter { eq("house_id", houseId) }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<HouseAuditLog>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
