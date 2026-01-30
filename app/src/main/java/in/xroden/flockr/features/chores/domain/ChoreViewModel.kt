package `in`.xroden.flockr.features.chores.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chores.data.ChoreRepository
import `in`.xroden.flockr.features.chores.ui.ChoreFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class ChoreViewModel @Inject constructor(
    private val choreRepository: ChoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChoreUiState>(ChoreUiState.Loading)
    val uiState: StateFlow<ChoreUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreateChoreUiState>(CreateChoreUiState.Idle)
    val createState: StateFlow<CreateChoreUiState> = _createState.asStateFlow()

    private val _filterOption = MutableStateFlow(ChoreFilter.ALL)
    val filterOption: StateFlow<ChoreFilter> = _filterOption.asStateFlow()

    private var currentHouseId: String? = null
    private var choreJob: kotlinx.coroutines.Job? = null

    fun setFilter(filter: ChoreFilter) {
        _filterOption.value = filter
    }

    fun loadChores(houseId: String) {
        // Skip if already loading the same house
        if (currentHouseId == houseId && choreJob?.isActive == true) return

        choreJob?.cancel()
        currentHouseId = houseId

        choreJob = viewModelScope.launch {
            // Only show loading on first load
            if (_uiState.value !is ChoreUiState.Success) {
                _uiState.value = ChoreUiState.Loading
            }

            choreRepository.getChoresFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { chores ->
                        val active = chores.filter { !it.isCompleted }
                        val completed = chores.filter { it.isCompleted }
                        
                        _uiState.value = ChoreUiState.Success(
                            allChores = chores,
                            activeChores = active,
                            completedChores = completed
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = ChoreUiState.Error(
                            message = error.message ?: "Failed to load chores",
                            cause = error
                        )
                    }
                )
            }
        }
    }    fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: `in`.xroden.flockr.data.enums.ChoreRecurrence?,
        assignedTo: String?
    ) {
        viewModelScope.launch {
            _createState.value = CreateChoreUiState.Loading
            
            choreRepository.createChore(
                houseId = houseId,
                taskName = taskName,
                description = description,
                dueDate = dueDate,
                recurrencePattern = recurrencePattern,
                assignedTo = assignedTo
            ).fold(
                onSuccess = {
                    _createState.value = CreateChoreUiState.Success
                    kotlinx.coroutines.delay(1000)
                    _createState.value = CreateChoreUiState.Idle
                },
                onFailure = { error ->
                    _createState.value = CreateChoreUiState.Error(
                        message = error.message ?: "Failed to create chore"
                    )
                }
            )
        }
    }

    fun updateChore(
        choreId: String,
        taskName: String?,
        description: String?,
        dueDate: LocalDate?,
        assignedTo: String?
    ) {
        viewModelScope.launch {
            choreRepository.updateChore(
                choreId = choreId,
                taskName = taskName,
                description = description,
                dueDate = dueDate,
                assignedTo = assignedTo
            ).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = ChoreUiState.Error(
                        message = error.message ?: "Failed to update chore",
                        cause = error
                    )
                }
            )
        }
    }

    fun completeChore(choreId: String, houseId: String, taskName: String) {
        viewModelScope.launch {
            // Optimistic update BEFORE server call for snappier UI
            val currentState = _uiState.value
            if (currentState is ChoreUiState.Success) {
                val now = Clock.System.now()
                val updatedAll = currentState.allChores.map { chore ->
                    if (chore.id == choreId) {
                        chore.copy(isCompleted = true, completedAt = now)
                    } else chore
                }
                _uiState.value = currentState.copy(
                    allChores = updatedAll,
                    activeChores = updatedAll.filter { !it.isCompleted },
                    completedChores = updatedAll.filter { it.isCompleted }
                )
            }

            choreRepository.completeChore(choreId, houseId).fold(
                onSuccess = {
                    // Server confirmed - realtime flow will sync
                },
                onFailure = { error ->
                    // Revert optimistic update on failure
                    loadChores(houseId)
                    _uiState.value = ChoreUiState.Error(
                        message = error.message ?: "Failed to complete chore",
                        cause = error
                    )
                }
            )
        }
    }

    fun deleteChore(choreId: String, houseId: String) {
        viewModelScope.launch {
            choreRepository.deleteChore(choreId, houseId).fold(
                onSuccess = {
                    // Optimistically update UI
                    val currentState = _uiState.value
                    if (currentState is ChoreUiState.Success) {
                        val updatedAll = currentState.allChores.filter { it.id != choreId }
                        _uiState.value = currentState.copy(
                            allChores = updatedAll,
                            activeChores = updatedAll.filter { !it.isCompleted },
                            completedChores = updatedAll.filter { it.isCompleted }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = ChoreUiState.Error(
                        message = error.message ?: "Failed to delete chore",
                        cause = error
                    )
                }
            )
        }
    }

    fun clearCompletedChores(houseId: String) {
        viewModelScope.launch {
            choreRepository.clearCompletedChores(houseId).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = ChoreUiState.Error(
                        message = error.message ?: "Failed to clear completed chores",
                        cause = error
                    )
                }
            )
        }
    }

    fun resetCreateState() {
        _createState.value = CreateChoreUiState.Idle
    }
}
