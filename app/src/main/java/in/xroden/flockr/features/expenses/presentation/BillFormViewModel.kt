/** Adding or editing a recurring bill: its amount, schedule, reminders and split. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.SplitMethod
import `in`.xroden.flockr.features.expenses.data.RecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.data.expenseShares
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.RecurringShare
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The reminder lead times a bill can choose, in days before it is due. */
val REMINDER_DAY_OPTIONS = listOf(0, 1, 2, 3, 5, 7, 14)

data class BillFormState(
    val billId: String? = null,
    val name: String = "",
    val amount: String = "",
    val category: String = "Rent",
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
    val customFrequencyDays: String = "",
    val firstDueDate: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val allowPrepayment: Boolean = false,
    val split: SplitDraft = SplitDraft(),
    val notes: String = "",
    val members: List<MemberWithProfile> = emptyList(),
    val viewerId: String = "",
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = billId != null

    val parsedAmount: BigDecimal? get() = parseMoney(amount, currencyCode)?.takeIf { it.signum() > 0 }

    /** Days between payments for a custom schedule, or null while not a whole number from 1 to 366. */
    val parsedCustomDays: Int? get() = customFrequencyDays.trim().toIntOrNull()?.takeIf { it in 1..366 }

    /** What each person would owe if the viewer paid this amount, which is also how the split is checked. */
    val preview: List<ExpenseShare>?
        get() {
            val total = parsedAmount ?: return null
            val values = if (split.isEnabled) split.parsedValues(currencyCode) ?: return null else emptyMap()
            return expenseShares(total, viewerId, split.savedMethod, values, minorUnitDigits(currencyCode))
        }

    val unassigned: BigDecimal? get() = split.unassigned(parsedAmount, currencyCode)

    val canSave: Boolean
        get() = isLoaded && !isSaving && name.isNotBlank() && firstDueDate != null && preview != null &&
            (frequency != ExpenseFrequency.CUSTOM || parsedCustomDays != null)

    /** The split as the bill stores it; an equal split stores a weight of one for each person. */
    fun recurringShares(): List<RecurringShare> =
        if (!split.isEnabled) emptyList() else split.parsedValues(currencyCode).orEmpty().map { (userId, value) -> RecurringShare(userId, value) }

    companion object {
        fun editing(bill: RecurringExpense, base: BillFormState): BillFormState = base.copy(
            billId = bill.id,
            name = bill.name,
            amount = bill.amount.toAmountInput(base.currencyCode),
            category = bill.category,
            frequency = bill.frequency,
            customFrequencyDays = bill.customFrequencyDays?.toString().orEmpty(),
            firstDueDate = bill.firstDueDate,
            reminderEnabled = bill.reminderEnabled,
            reminderDaysBefore = bill.reminderDaysBefore,
            allowPrepayment = bill.allowPrepayment,
            split = SplitDraft(
                isEnabled = bill.splitMethod != null,
                method = bill.splitMethod ?: SplitMethod.EQUAL,
                participantIds = bill.shares.map { it.userId }.toSet().ifEmpty { base.split.participantIds },
                values = if (bill.splitMethod == SplitMethod.EQUAL) emptyMap() else bill.shares.associate { share ->
                    share.userId to if (bill.splitMethod == SplitMethod.EXACT) share.splitValue.toAmountInput(base.currencyCode) else share.splitValue.stripTrailingZeros().toPlainString()
                },
            ),
            notes = bill.notes.orEmpty(),
            isLoaded = true,
        )
    }
}

@HiltViewModel
class BillFormViewModel @Inject constructor(
    private val billRepository: RecurringExpenseRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(BillFormState())
    val form: StateFlow<BillFormState> = _form.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun initialize(houseId: String, billId: String?) {
        if (_form.value.isLoaded) return
        viewModelScope.launch {
            val config = async { houseRepository.getHouseConfig(houseId).getOrNull() }
            val members = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
            val base = BillFormState(
                firstDueDate = config.await().today(),
                split = SplitDraft.everyone(members).copy(isEnabled = true),
                members = members,
                viewerId = houseRepository.getCurrentUserId().orEmpty(),
                currencyCode = config.await().currency(),
                isLoaded = billId == null,
            )
            _form.value = base
            if (billId == null) return@launch
            billRepository.getRecurringExpenses(houseId).fold(
                onSuccess = { bills ->
                    val bill = bills.firstOrNull { it.id == billId }
                    _form.value = if (bill != null) BillFormState.editing(bill, base) else base.copy(error = "This bill no longer exists")
                },
                onFailure = { error -> _form.value = base.copy(error = error.userMessage()) },
            )
        }
    }

    fun update(transform: (BillFormState) -> BillFormState) = _form.update(transform)
    fun onSplitEnabledChange(enabled: Boolean) = _form.update { it.copy(split = it.split.copy(isEnabled = enabled)) }
    fun onSplitMethodChange(method: SplitMethod) = _form.update { it.copy(split = it.split.withMethod(method, it.members)) }
    fun onParticipantChange(userId: String, isIncluded: Boolean) = _form.update { it.copy(split = it.split.withParticipant(userId, isIncluded)) }
    fun onSplitValueChange(userId: String, value: String) = _form.update { it.copy(split = it.split.withValue(userId, value)) }
    fun dismissError() = _form.update { it.copy(error = null) }

    fun save(houseId: String) {
        val form = _form.value
        if (!form.canSave) return
        val amount = form.parsedAmount ?: return
        val firstDueDate = form.firstDueDate ?: return
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            billRepository.saveRecurringExpense(
                houseId = houseId,
                billId = form.billId,
                name = form.name,
                amount = amount,
                category = form.category,
                frequency = form.frequency,
                customFrequencyDays = form.parsedCustomDays,
                firstDueDate = firstDueDate,
                reminderEnabled = form.reminderEnabled,
                reminderDaysBefore = form.reminderDaysBefore,
                allowPrepayment = form.allowPrepayment,
                splitMethod = form.split.savedMethod,
                shares = form.recurringShares(),
                notes = form.notes,
            ).fold(
                onSuccess = { _saved.send(Unit) },
                onFailure = { error -> _form.update { it.copy(isSaving = false, error = error.userMessage()) } },
            )
        }
    }
}
