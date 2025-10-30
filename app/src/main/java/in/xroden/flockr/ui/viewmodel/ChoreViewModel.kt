package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Chore
import `in`.xroden.flockr.data.repository.ChoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChoreViewModel @Inject constructor(
    private val choreRepository: ChoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChoreUiState>(ChoreUiState.Loading)
    val uiState: StateFlow<ChoreUiState> = _uiState.asStateFlow()

    fun loadChores(houseId: String) {
        viewModelScope.launch {
            try {
                choreRepository.getChoresFlow(houseId).collect { chores ->
                    _uiState.value = ChoreUiState.Success(chores)
                }
            } catch (e: Exception) {
                _uiState.value = ChoreUiState.Error(e.message ?: "Failed to load chores")
            }
        }
    }

    fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: String?,
        isRecurring: Boolean,
        assignedTo: String?
    ) {
        viewModelScope.launch {
            choreRepository.createChore(
                houseId = houseId,
                taskName = taskName,
                description = description,
                dueDate = dueDate,
                isRecurring = isRecurring,
                recurrencePattern = null,
                assignedTo = assignedTo
            )
        }
    }

    fun completeChore(choreId: String, houseId: String, taskName: String) {
        viewModelScope.launch {
            choreRepository.completeChore(choreId, houseId, taskName)
        }
    }

    fun deleteChore(choreId: String) {
        viewModelScope.launch {
            choreRepository.deleteChore(choreId)
        }
    }
}

sealed class ChoreUiState {
    object Loading : ChoreUiState()
    data class Success(val chores: List<Chore>) : ChoreUiState()
    data class Error(val message: String) : ChoreUiState()
}

