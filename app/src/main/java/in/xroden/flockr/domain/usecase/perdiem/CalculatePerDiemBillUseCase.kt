package `in`.xroden.flockr.domain.usecase.perdiem

import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case to calculate per-diem bills from entries
 * Note: These calculations are typically done by Supabase RPC functions,
 * but this UseCase provides client-side calculation capabilities for offline scenarios
 * or validation purposes.
 */
class CalculatePerDiemBillUseCase @Inject constructor() {

    /**
     * Calculate itemized bill (grouped by item)
     * 
     * @param entries List of per-diem entries with details
     * @return List of itemized bill items
     */
    fun calculateItemized(entries: List<PerDiemEntryWithDetails>): List<PerDiemBillItemized> {
        return entries
            .groupBy { it.itemName }
            .map { (itemName, itemEntries) ->
                val totalQuantity = itemEntries.sumOf { it.quantity }
                val rate = itemEntries.first().rate
                val unit = itemEntries.first().unit
                
                PerDiemBillItemized(
                    itemName = itemName,
                    totalQuantity = totalQuantity,
                    rate = rate,
                    unit = unit,
                    totalAmount = totalQuantity * rate
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Calculate bill by member (grouped by user)
     * 
     * @param entries List of per-diem entries with details
     * @return List of member bills
     */
    fun calculateByMember(entries: List<PerDiemEntryWithDetails>): List<PerDiemBillByMember> {
        return entries
            .groupBy { it.addedBy }
            .map { (userId, memberEntries) ->
                val fullName = memberEntries.first().userName
                val totalQuantity = memberEntries.sumOf { it.quantity }
                val totalAmount = memberEntries.sumOf { it.totalCost }
                
                PerDiemBillByMember(
                    userId = userId,
                    fullName = fullName,
                    totalQuantity = totalQuantity,
                    totalAmount = totalAmount
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Calculate grand total for all entries
     */
    fun calculateGrandTotal(entries: List<PerDiemEntryWithDetails>): BigDecimal {
        return entries.sumOf { it.totalCost }
    }
}

