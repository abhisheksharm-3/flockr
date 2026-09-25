/** The payments recorded against one recurring bill. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.RecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BillHistoryUiState {
    data object Loading : BillHistoryUiState
    data class Error(val message: String) : BillHistoryUiState
    data class Ready(
        val billName: String?,
        val payments: List<Expense>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
    ) : BillHistoryUiState
}

@HiltViewModel
class BillHistoryViewModel @Inject constructor(
    private val billRepository: RecurringExpenseRepository,
    private val houseRepository: IHouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BillHistoryUiState>(BillHistoryUiState.Loading)
    val state: StateFlow<BillHistoryUiState> = _state.asStateFlow()

    fun load(houseId: String, billId: String) {
        viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            val billName = async { billRepository.getRecurringExpenses(houseId).getOrNull()?.firstOrNull { it.id == billId }?.name }
            _state.value = billRepository.getBillPayments(billId).fold(
                onSuccess = { BillHistoryUiState.Ready(billName.await(), it, members.await(), houseRepository.getCurrentUserId().orEmpty()) },
                onFailure = { BillHistoryUiState.Error(it.userMessage()) },
            )
        }
    }
}
