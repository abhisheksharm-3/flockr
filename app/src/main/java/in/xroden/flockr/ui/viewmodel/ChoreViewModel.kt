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
            _uiState.value = ChoreUiState.Loading
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
        assignedTo: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ChoreViewModel", "Creating chore: $taskName for house: $houseId")
                val result = choreRepository.createChore(
                    houseId = houseId,
                    taskName = taskName,
                    description = description,
                    dueDate = dueDate,
                    isRecurring = isRecurring,
                    recurrencePattern = null,
                    assignedTo = assignedTo
                )
                result.fold(
                    onSuccess = {
                        android.util.Log.d("ChoreViewModel", "Chore created successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        val errorMessage = error.message ?: "Failed to create chore"
                        android.util.Log.e("ChoreViewModel", "Failed to create chore: $errorMessage", error)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to create chore"
                android.util.Log.e("ChoreViewModel", "Exception creating chore: $errorMessage", e)
                onError(errorMessage)
            }
        }
    }

    fun completeChore(choreId: String, houseId: String, taskName: String) {
        viewModelScope.launch {
            choreRepository.completeChore(choreId, houseId, taskName)
        }
    }

    fun updateChore(
        choreId: String,
        taskName: String,
        description: String?,
        dueDate: String?,
        assignedTo: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ChoreViewModel", "Updating chore: $choreId")
                val result = choreRepository.updateChore(
                    choreId = choreId,
                    taskName = taskName,
                    description = description,
                    dueDate = dueDate,
                    assignedTo = assignedTo
                )
                result.fold(
                    onSuccess = {
                        android.util.Log.d("ChoreViewModel", "Chore updated successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        val errorMessage = error.message ?: "Failed to update chore"
                        android.util.Log.e("ChoreViewModel", "Failed to update chore: $errorMessage", error)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to update chore"
                android.util.Log.e("ChoreViewModel", "Exception updating chore: $errorMessage", e)
                onError(errorMessage)
            }
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

