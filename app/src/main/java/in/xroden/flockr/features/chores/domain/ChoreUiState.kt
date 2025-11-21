package `in`.xroden.flockr.features.chores.domain

import `in`.xroden.flockr.features.chores.model.Chore

sealed interface ChoreUiState {
    data object Loading : ChoreUiState
    data class Success(
        val allChores: List<Chore>,
        val activeChores: List<Chore>,
        val completedChores: List<Chore>
    ) : ChoreUiState
    data class Error(val message: String, val cause: Throwable? = null) : ChoreUiState
}

sealed interface CreateChoreUiState {
    data object Idle : CreateChoreUiState
    data object Loading : CreateChoreUiState
    data object Success : CreateChoreUiState
    data class Error(val message: String) : CreateChoreUiState
}


