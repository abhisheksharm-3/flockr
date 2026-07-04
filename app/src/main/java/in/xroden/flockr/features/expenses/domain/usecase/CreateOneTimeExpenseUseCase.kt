package `in`.xroden.flockr.features.expenses.domain.usecase

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.data.IExpenseRepository
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

class CreateOneTimeExpenseUseCase @Inject constructor(
    private val expenseRepository: IExpenseRepository
) {
    /**
     * Creates a one-time expense with proper validation and split handling.
     *
     * @param houseId The house where the expense belongs
     * @param name Name/description of the expense
     * @param amount Total amount of the expense
     * @param category Expense category
     * @param paidBy User ID who paid for the expense
     * @param date Date of the expense
     * @param notes Optional notes
     * @param splitWith List of user IDs to split with (empty = no split)
     * @param splitType How to split the expense (equal/custom)
     * @param customAmounts Custom split amounts (only for custom split type)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        houseId: String,
        name: String,
        amount: BigDecimal,
        category: String,
        paidBy: String,
        date: LocalDate,
        notes: String?,
        splitWith: List<String>,
        splitType: ExpenseSplitType?,
        customAmounts: Map<String, BigDecimal>?
    ): Result<Unit> {
        // Validation: Ensure amount is positive
        if (amount <= BigDecimal.ZERO) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }

        // Validation: for any non-equal split with explicit amounts (the UI emits AMOUNT,
        // not CUSTOM), the per-member amounts must sum to the total — otherwise the payer
        // silently absorbs the shortfall or is credited a surplus that nobody owes.
        if (splitType != null && splitType != ExpenseSplitType.EQUAL && customAmounts != null) {
            val totalCustom = customAmounts.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
            // compareTo, not !=, so scale differences (100.0 vs 100.00) don't reject a valid split.
            if (totalCustom.compareTo(amount) != 0) {
                return Result.failure(IllegalArgumentException("Split amounts must sum to the total amount"))
            }
        }

        return expenseRepository.createOneTimeExpense(
            houseId = houseId,
            name = name,
            amount = amount,
            category = category,
            paidBy = paidBy,
            date = date,
            notes = notes,
            splitWith = splitWith,
            splitType = splitType,
            customAmounts = customAmounts
        )
    }
}
