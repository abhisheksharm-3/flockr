/** Everyone's balance in a house, the plan that settles them, and the history behind any one pair. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.model.HouseStanding
import `in`.xroden.flockr.features.expenses.model.SharedHistoryEntry
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BalancesUiState {
    data object Loading : BalancesUiState
    data class Error(val message: String) : BalancesUiState

    /** [history] holds the shared history of each member the viewer has opened, keyed by their id. */
    data class Ready(
        val standing: HouseStanding,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
        val history: Map<String, List<SharedHistoryEntry>> = emptyMap(),
    ) : BalancesUiState
}

@HiltViewModel
class BalancesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BalancesUiState>(BalancesUiState.Loading)
    val state: StateFlow<BalancesUiState> = _state.asStateFlow()

    fun load(houseId: String) {
        viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId) }
            _state.value = expenseRepository.getStanding(houseId).fold(
                onSuccess = { standing ->
                    BalancesUiState.Ready(
                        standing = standing,
                        members = members.await().getOrElse { emptyList() }.associateBy { it.userId },
                        viewerId = expenseRepository.getCurrentUserId().orEmpty(),
                    )
                },
                onFailure = { BalancesUiState.Error(it.userMessage()) },
            )
        }
    }

    /** Loads what the viewer and [otherUserId] have shared, once per member. */
    fun loadHistory(houseId: String, otherUserId: String) {
        val ready = _state.value as? BalancesUiState.Ready ?: return
        if (otherUserId in ready.history || otherUserId == ready.viewerId) return
        viewModelScope.launch {
            expenseRepository.getSharedHistory(houseId, otherUserId).onSuccess { entries ->
                _state.update { current -> (current as? BalancesUiState.Ready)?.let { it.copy(history = it.history + (otherUserId to entries)) } ?: current }
            }
        }
    }
}
