/** A house's recurring bills and recording a payment against one. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.expenses.data.RecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.utils.minorUnitDigits
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface BillsUiState {
    data object Loading : BillsUiState
    data class Error(val message: String) : BillsUiState
    data class Ready(
        val bills: List<RecurringExpense>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
        val currencyCode: String,
        val payingBillId: String? = null,
    ) : BillsUiState
}

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val billRepository: RecurringExpenseRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BillsUiState>(BillsUiState.Loading)
    val state: StateFlow<BillsUiState> = _state.asStateFlow()

    private val _events = Channel<Notice>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun load(houseId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            val currency = async { houseRepository.getHouseConfig(houseId).getOrNull().currency() }
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            billRepository.getRecurringExpensesFlow(houseId).collect { result ->
                _state.value = result.fold(
                    onSuccess = { BillsUiState.Ready(it, members.await(), viewerId, currency.await()) },
                    onFailure = { BillsUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    /** Records that the signed-in user paid [amount] towards [bill] on [date], split as the bill is. */
    fun pay(bill: RecurringExpense, amount: BigDecimal, date: LocalDate) {
        val ready = _state.value as? BillsUiState.Ready ?: return
        if (ready.payingBillId != null) return
        _state.value = ready.copy(payingBillId = bill.id)
        viewModelScope.launch {
            val result = billRepository.payRecurringExpense(bill, amount, date, minorUnitDigits(ready.currencyCode))
            (_state.value as? BillsUiState.Ready)?.let { _state.value = it.copy(payingBillId = null) }
            _events.send(
                result.fold(
                    onSuccess = { Notice("${bill.name} marked as paid", isError = false) },
                    onFailure = { Notice(it.userMessage(), isError = true) },
                )
            )
        }
    }

    fun delete(bill: RecurringExpense) {
        viewModelScope.launch {
            billRepository.deleteRecurringExpense(bill.id).onFailure { _events.send(Notice(it.userMessage(), isError = true)) }
        }
    }
}
