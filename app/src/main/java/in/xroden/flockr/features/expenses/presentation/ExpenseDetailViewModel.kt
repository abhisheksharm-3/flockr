/** One expense or payment: who paid, who owes what, and deleting it. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ExpenseDetailUiState {
    data object Loading : ExpenseDetailUiState
    data class Error(val message: String) : ExpenseDetailUiState

    /** [canDelete] mirrors the database rule: whoever added it, or an admin. */
    data class Ready(
        val expense: Expense,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
        val canDelete: Boolean,
        val isDeleting: Boolean = false,
        val deleteError: String? = null,
    ) : ExpenseDetailUiState
}

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: IHouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val state: StateFlow<ExpenseDetailUiState> = _state.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)

    /** Emits once the expense is gone, which is when the screen should close. */
    val deleted = _deleted.receiveAsFlow()

    fun load(houseId: String, expenseId: String) {
        viewModelScope.launch {
            _state.value = ExpenseDetailUiState.Loading
            val members = async { houseRepository.getHouseMembers(houseId) }
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            _state.value = expenseRepository.getExpense(expenseId).fold(
                onSuccess = { expense ->
                    val roster = members.await().getOrElse { emptyList() }.associateBy { it.userId }
                    val viewerRole = roster[viewerId]?.role
                    ExpenseDetailUiState.Ready(
                        expense = expense,
                        members = roster,
                        viewerId = viewerId,
                        canDelete = expense.createdBy == viewerId || viewerRole == HouseMemberRole.OWNER || viewerRole == HouseMemberRole.ADMIN,
                    )
                },
                onFailure = { ExpenseDetailUiState.Error(it.userMessage()) },
            )
        }
    }

    fun delete() {
        val ready = _state.value as? ExpenseDetailUiState.Ready ?: return
        if (ready.isDeleting) return
        _state.value = ready.copy(isDeleting = true, deleteError = null)
        viewModelScope.launch {
            expenseRepository.deleteExpense(ready.expense.id).fold(
                onSuccess = { _deleted.send(Unit) },
                onFailure = { _state.value = ready.copy(isDeleting = false, deleteError = it.userMessage()) },
            )
        }
    }

    fun dismissDeleteError() {
        val ready = _state.value as? ExpenseDetailUiState.Ready ?: return
        _state.value = ready.copy(deleteError = null)
    }
}
