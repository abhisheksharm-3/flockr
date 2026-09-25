/** The expense form's state, shared by adding and editing so both follow the same split rules. */
package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.data.SplitShares
import `in`.xroden.flockr.features.expenses.data.planSplit
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.toAmountInput
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class ExpenseFormState(
    val expenseId: String? = null,
    val name: String = "",
    val amount: String = "",
    val date: LocalDate? = null,
    val notes: String = "",
    val category: String = DEFAULT_EXPENSE_CATEGORY,
    val isSplitEnabled: Boolean = false,
    val isSplitEqual: Boolean = true,
    val selectedMemberIds: Set<String> = emptySet(),
    val customSplits: Map<String, String> = emptyMap(),
    val houseMembers: List<MemberWithProfile> = emptyList(),
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val payerId: String = "",
    val viewerId: String = "",
    val isLoaded: Boolean = false,
) {
    val isEditing: Boolean get() = expenseId != null

    /** Whether the person filling the form is the one who paid, which decides how shares are worded. */
    val isViewerPayer: Boolean get() = payerId == viewerId

    /** The typed amount, or null while it is not a valid positive amount in the house currency. */
    val parsedAmount: BigDecimal? get() = parseMoney(amount, currencyCode)?.takeIf { it.signum() > 0 }

    /** The split type the form describes, or null when the expense is not split. */
    val splitType: ExpenseSplitType?
        get() = when {
            !isSplitEnabled || selectedMemberIds.isEmpty() -> null
            isSplitEqual -> ExpenseSplitType.EQUAL
            else -> ExpenseSplitType.AMOUNT
        }

    /**
     * How the expense divides as currently filled in, or null while the amount or a custom share is
     * missing or invalid. The preview and the save both read this, so they cannot disagree.
     */
    internal val splitPlan: SplitShares?
        get() = parsedAmount?.let { total ->
            planSplit(
                amount = total,
                payerId = payerId,
                splitType = splitType,
                members = selectedMemberIds,
                customAmounts = customSplits.mapNotNull { (userId, text) ->
                    parseMoney(text, currencyCode)?.let { userId to it }
                }.toMap(),
                minorDigits = minorUnitDigits(currencyCode),
            )
        }

    val canSave: Boolean get() = isLoaded && name.isNotBlank() && date != null && splitPlan != null

    companion object {
        /**
         * The form for editing [expense]. Its split reads as equal only when an equal split of the
         * same members reproduces the stored rows exactly; anything else opens as custom amounts, so
         * reopening and saving never changes who owes what.
         */
        fun editing(expense: OneTimeExpense, base: ExpenseFormState): ExpenseFormState {
            val rows = expense.splits.orEmpty().associate { it.userId to it.amountOwed }
            val digits = minorUnitDigits(base.currencyCode)
            val equalRows = planSplit(expense.amount, expense.paidBy, ExpenseSplitType.EQUAL, rows.keys, null, digits)?.rows
            val isEqual = equalRows != null && equalRows.keys == rows.keys &&
                rows.all { (userId, owed) -> equalRows.getValue(userId).compareTo(owed) == 0 }
            return base.copy(
                expenseId = expense.id,
                name = expense.name,
                amount = expense.amount.toAmountInput(base.currencyCode),
                date = expense.date,
                notes = expense.notes.orEmpty(),
                category = expense.category,
                isSplitEnabled = rows.isNotEmpty(),
                isSplitEqual = isEqual,
                selectedMemberIds = rows.keys,
                customSplits = if (isEqual) emptyMap() else rows.mapValues { it.value.toAmountInput(base.currencyCode) },
                payerId = expense.paidBy,
                isLoaded = true,
            )
        }
    }
}

const val DEFAULT_EXPENSE_CATEGORY = "Groceries"

sealed interface ExpenseFormUiState {
    data object Idle : ExpenseFormUiState
    data object Saving : ExpenseFormUiState
    data class Error(val message: String) : ExpenseFormUiState
}
