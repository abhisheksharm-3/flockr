/** Recording a payment between the signed-in user and one housemate, in either direction. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.today
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

/**
 * [isViewerPaying] is the direction: true when the viewer paid [otherUserId], false when they were
 * paid. [plan] is the house's settle-up plan, from which [suggestedAmount] is read.
 */
data class SettleUpFormState(
    val viewerId: String = "",
    val members: List<MemberWithProfile> = emptyList(),
    val otherUserId: String? = null,
    val isViewerPaying: Boolean = true,
    val amount: String = "",
    val date: LocalDate? = null,
    val note: String = "",
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val plan: List<SettleUpPayment> = emptyList(),
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val parsedAmount: BigDecimal? get() = parseMoney(amount, currencyCode)?.takeIf { it.signum() > 0 }

    /** What the plan says should pass between the two in the chosen direction, if anything. */
    val suggestedAmount: BigDecimal?
        get() = plan.firstOrNull {
            if (isViewerPaying) it.fromUserId == viewerId && it.toUserId == otherUserId else it.fromUserId == otherUserId && it.toUserId == viewerId
        }?.amount

    val canSave: Boolean get() = isLoaded && !isSaving && otherUserId != null && parsedAmount != null && date != null
}

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: IHouseRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(SettleUpFormState())
    val form: StateFlow<SettleUpFormState> = _form.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    /**
     * Prefills from a suggested payment when one is given; otherwise from the first payment the
     * plan asks of the viewer, so "Settle up" usually opens ready to save.
     */
    fun initialize(houseId: String, fromUserId: String?, toUserId: String?, amount: BigDecimal?) {
        if (_form.value.isLoaded) return
        viewModelScope.launch {
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val config = async { houseRepository.getHouseConfig(houseId).getOrNull() }
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() } }
            val plan = expenseRepository.getStanding(houseId).getOrNull()?.paymentsOf(viewerId).orEmpty()
            val currency = config.await().currency()
            val suggestion = when {
                fromUserId != null && toUserId != null -> SettleUpPayment(fromUserId, toUserId, amount ?: BigDecimal.ZERO)
                else -> plan.firstOrNull()
            }
            val isViewerPaying = suggestion?.toUserId != viewerId
            _form.value = SettleUpFormState(
                viewerId = viewerId,
                members = members.await().filter { it.userId != viewerId && it.isActive },
                otherUserId = suggestion?.let { if (isViewerPaying) it.toUserId else it.fromUserId },
                isViewerPaying = isViewerPaying,
                amount = suggestion?.amount?.takeIf { it.signum() > 0 }?.toAmountInput(currency).orEmpty(),
                date = config.await().today(),
                currencyCode = currency,
                plan = plan,
                isLoaded = true,
            )
        }
    }

    fun onOtherChange(userId: String) = _form.update { it.copy(otherUserId = userId).withSuggestedAmount() }
    fun onDirectionChange(isViewerPaying: Boolean) = _form.update { it.copy(isViewerPaying = isViewerPaying).withSuggestedAmount() }
    fun onAmountChange(amount: String) = _form.update { it.copy(amount = amount) }
    fun onDateChange(date: LocalDate) = _form.update { it.copy(date = date) }
    fun onNoteChange(note: String) = _form.update { it.copy(note = note) }
    fun dismissError() = _form.update { it.copy(error = null) }

    private fun SettleUpFormState.withSuggestedAmount() =
        suggestedAmount?.let { copy(amount = it.toAmountInput(currencyCode)) } ?: this

    fun save(houseId: String) {
        val form = _form.value
        if (!form.canSave) return
        val other = form.otherUserId ?: return
        val amount = form.parsedAmount ?: return
        val date = form.date ?: return
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val (from, to) = if (form.isViewerPaying) form.viewerId to other else other to form.viewerId
            expenseRepository.settleUp(houseId, from, to, amount, date, form.note).fold(
                onSuccess = { _saved.send(Unit) },
                onFailure = { error -> _form.update { it.copy(isSaving = false, error = error.userMessage()) } },
            )
        }
    }
}
