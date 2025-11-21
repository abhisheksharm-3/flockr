package `in`.xroden.flockr.domain.usecase.expense

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Use case to calculate expense splits among members
 */
class CalculateExpenseSplitUseCase @Inject constructor() {

    /**
     * Calculate split amounts for an expense
     * 
     * @param totalAmount Total expense amount
     * @param splitType Type of split (equal, percentage, amount)
     * @param memberIds List of member IDs to split among
     * @param customSplits Custom split values (percentages or amounts) if applicable
     * @return Map of member ID to amount owed
     */
    operator fun invoke(
        totalAmount: BigDecimal,
        splitType: ExpenseSplitType,
        memberIds: List<String>,
        customSplits: Map<String, BigDecimal>? = null
    ): Result<Map<String, BigDecimal>> {
        return try {
            when (splitType) {
                ExpenseSplitType.EQUAL -> {
                    val amountPerPerson = totalAmount.divide(
                        BigDecimal(memberIds.size),
                        2,
                        RoundingMode.HALF_UP
                    )
                    Result.success(memberIds.associateWith { amountPerPerson })
                }

                ExpenseSplitType.CUSTOM -> {
                    // For custom splits, treat as EQUAL if no custom splits provided
                    if (customSplits == null) {
                        val amountPerPerson = totalAmount.divide(
                            BigDecimal(memberIds.size),
                            2,
                            RoundingMode.HALF_UP
                        )
                        Result.success(memberIds.associateWith { amountPerPerson })
                    } else {
                        Result.success(customSplits)
                    }
                }

                ExpenseSplitType.PERCENTAGE -> {
                    if (customSplits == null) {
                        return Result.failure(Exception("Custom splits required for percentage split"))
                    }
                    
                    // Validate percentages sum to 100
                    val totalPercentage = customSplits.values.fold(BigDecimal.ZERO) { acc, percent ->
                        acc + percent
                    }
                    
                    if (totalPercentage.compareTo(BigDecimal("100")) != 0) {
                        return Result.failure(Exception("Percentages must sum to 100%"))
                    }
                    
                    // Calculate amounts
                    val splits = customSplits.mapValues { (_, percentage) ->
                        totalAmount.multiply(percentage)
                            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    }
                    
                    Result.success(splits)
                }

                ExpenseSplitType.AMOUNT -> {
                    if (customSplits == null) {
                        return Result.failure(Exception("Custom splits required for amount split"))
                    }
                    
                    // Validate amounts sum to total
                    val totalSplit = customSplits.values.fold(BigDecimal.ZERO) { acc, amount ->
                        acc + amount
                    }
                    
                    if (totalSplit.compareTo(totalAmount) != 0) {
                        return Result.failure(Exception("Split amounts must equal total amount"))
                    }
                    
                    Result.success(customSplits)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


