/** The expenses hub: the house's activity with where the signed-in user stands. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.HouseStanding
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState
    data class Error(val message: String) : ExpensesUiState

    /** [members] includes past members, so old expenses still name who was on them. */
    data class Ready(
        val expenses: List<Expense>,
        val members: Map<String, MemberWithProfile>,
        val standing: HouseStanding,
        val viewerId: String,
    ) : ExpensesUiState
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Follows the house's expenses, re-reading balances and members each time they change. */
    fun load(houseId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            expenseRepository.getExpensesFlow(houseId).collect { result ->
                _state.value = result.fold(
                    onSuccess = { expenses -> ready(houseId, expenses) },
                    onFailure = { ExpensesUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    private suspend fun ready(houseId: String, expenses: List<Expense>): ExpensesUiState = coroutineScope {
        val standing = async { expenseRepository.getStanding(houseId) }
        val members = async { houseRepository.getHouseMembers(houseId) }
        runCatching {
            ExpensesUiState.Ready(
                expenses = expenses,
                members = members.await().getOrThrow().associateBy { it.userId },
                standing = standing.await().getOrThrow(),
                viewerId = expenseRepository.getCurrentUserId().orEmpty(),
            )
        }.getOrElse { ExpensesUiState.Error(it.userMessage()) }
    }
}
