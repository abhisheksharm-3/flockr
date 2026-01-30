package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntry
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/**
 * Repository interface for per-diem expense operations.
 * Enables easy mocking for unit tests.
 */
interface IPerDiemRepository {
    suspend fun getPerDiemConfigs(houseId: String): Result<List<PerDiemConfig>>
    suspend fun getPerDiemEntries(houseId: String, configId: String? = null): Result<List<PerDiemEntry>>
    suspend fun createPerDiemConfig(
        houseId: String,
        itemName: String,
        rate: BigDecimal,
        category: String,
        unit: String
    ): Result<PerDiemConfig>
    suspend fun updatePerDiemConfig(
        configId: String,
        itemName: String?,
        rate: BigDecimal?,
        category: String?,
        unit: String?
    ): Result<Unit>
    suspend fun deletePerDiemConfig(configId: String, deleteUsage: Boolean = false): Result<Unit>
    suspend fun addPerDiemEntry(
        houseId: String,
        configId: String,
        quantity: BigDecimal,
        date: LocalDate,
        itemName: String,
        notes: String? = null
    ): Result<PerDiemEntry>
    suspend fun deletePerDiemEntry(entryId: String): Result<Unit>
    suspend fun updatePerDiemEntry(
        entryId: String,
        quantity: BigDecimal?,
        date: LocalDate?,
        notes: String?
    ): Result<Unit>
    suspend fun getPerDiemBill(houseId: String, month: String): Result<List<PerDiemBillItemized>>
    suspend fun getPerDiemBillByMember(houseId: String, month: String): Result<List<PerDiemBillByMember>>
    suspend fun getPerDiemEntriesWithDetails(
        houseId: String,
        month: LocalDate? = null
    ): Result<List<PerDiemEntryWithDetails>>
}
