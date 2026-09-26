/** The expense form's state, shared by adding and editing so both follow the same split rules. */
package `in`.xroden.flockr.features.expenses.presentation

import android.net.Uri
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.features.expenses.data.expenseShares
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

data class ExpenseFormState(
    val expenseId: String? = null,
    val name: String = "",
    val amount: String = "",
    val date: LocalDate? = null,
    val notes: String = "",
    val category: String = DEFAULT_EXPENSE_CATEGORY,
    val split: SplitDraft = SplitDraft(),
    val houseMembers: List<MemberWithProfile> = emptyList(),
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val payerId: String = "",
    val viewerId: String = "",
    val isLoaded: Boolean = false,
    val receipt: Uri? = null,
    val hasSavedReceipt: Boolean = false,
) {
    val isEditing: Boolean get() = expenseId != null

    /** The typed amount, or null while it is not a valid positive amount in the house currency. */
    val parsedAmount: BigDecimal? get() = parseMoney(amount, currencyCode)?.takeIf { it.signum() > 0 }

    /**
     * The rows saving would write, or null while the amount or a split value is missing or invalid.
     * The preview and the save both read this, so they cannot disagree.
     */
    val shares: List<ExpenseShare>?
        get() {
            val total = parsedAmount ?: return null
            val values = if (split.isEnabled) split.parsedValues(currencyCode) ?: return null else emptyMap()
            return expenseShares(total, payerId, split.savedMethod, values, minorUnitDigits(currencyCode))
        }

    /** See [SplitDraft.unassigned]. */
    val unassigned: BigDecimal? get() = split.unassigned(parsedAmount, currencyCode)

    val canSave: Boolean get() = isLoaded && name.isNotBlank() && date != null && payerId.isNotEmpty() && shares != null

    companion object {
        /** The form reopened on [expense], with the split method and values it was saved with. */
        fun editing(expense: Expense, base: ExpenseFormState): ExpenseFormState {
            val method = expense.splitMethod
            val participants = expense.shares
                .filter { if (method == SplitMethod.EQUAL) it.owedShare.signum() > 0 else it.splitValue != null }
                .map { it.userId }
                .toSet()
            val values = expense.shares.mapNotNull { share ->
                val value = share.splitValue ?: return@mapNotNull null
                share.userId to if (method == SplitMethod.EXACT) value.toAmountInput(base.currencyCode) else value.stripTrailingZeros().toPlainString()
            }.toMap()
            return base.copy(
                expenseId = expense.id,
                name = expense.name,
                amount = expense.amount.toAmountInput(base.currencyCode),
                date = expense.date,
                notes = expense.notes.orEmpty(),
                hasSavedReceipt = expense.receiptPath != null,
                category = expense.category ?: DEFAULT_EXPENSE_CATEGORY,
                split = SplitDraft(
                    isEnabled = method != null,
                    method = method ?: SplitMethod.EQUAL,
                    participantIds = participants.ifEmpty { base.split.participantIds },
                    values = values,
                ),
                payerId = expense.payerId ?: base.payerId,
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
