package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.features.house.model.HouseAuditLog

interface IHouseAuditRepository {
    suspend fun getHouseAuditLogs(houseId: String): List<HouseAuditLog>
    suspend fun getRecentAuditLogs(houseId: String, limit: Int = 10): List<HouseAuditLog>
}
