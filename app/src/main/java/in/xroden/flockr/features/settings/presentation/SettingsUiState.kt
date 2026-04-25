package `in`.xroden.flockr.features.settings.presentation

import `in`.xroden.flockr.features.auth.model.Profile

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: Profile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface UpdateProfileUiState {
    data object Idle : UpdateProfileUiState
    data object Loading : UpdateProfileUiState
    data object Success : UpdateProfileUiState
    data class Error(val message: String) : UpdateProfileUiState
}


