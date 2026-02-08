package `in`.xroden.flockr.features.expenses.domain.usecase

import `in`.xroden.flockr.features.expenses.data.IPerDiemRepository
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import javax.inject.Inject

/**
 * Use case for loading per-diem billing reports.
 * Coordinates fetching itemized and member-wise reports.
 */
class LoadPerDiemBillUseCase @Inject constructor(
    private val perDiemRepository: IPerDiemRepository
) {
    /**
     * Loads both itemized and member-wise per-diem billing reports for a month.
     *
     * @param houseId The house to load reports for
     * @param month The month in "YYYY-MM" format
     * @return Pair of itemized and member-wise reports, or error
     */
    suspend operator fun invoke(
        houseId: String,
        month: String
    ): Result<Pair<List<PerDiemBillItemized>, List<PerDiemBillByMember>>> {
        // Load both reports in parallel
        val itemizedResult = perDiemRepository.getPerDiemBill(houseId, month)
        val byMemberResult = perDiemRepository.getPerDiemBillByMember(houseId, month)

        // If both succeeded, return paired results
        if (itemizedResult.isSuccess && byMemberResult.isSuccess) {
            val itemized = itemizedResult.getOrElse { emptyList() }
            val byMember = byMemberResult.getOrElse { emptyList() }
            return Result.success(Pair(itemized, byMember))
        }

        // Return the first error encountered
        val error = itemizedResult.exceptionOrNull() ?: byMemberResult.exceptionOrNull()
        return Result.failure(error ?: Exception("Failed to load per-diem reports"))
    }
}
