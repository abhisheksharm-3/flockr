package `in`.xroden.flockr.features.settings.domain

import `in`.xroden.flockr.features.auth.model.Profile
import `in`.xroden.flockr.features.house.model.HouseConfig

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

sealed interface HouseSettingsUiState {
    data object Loading : HouseSettingsUiState
    data class Success(val config: HouseConfig) : HouseSettingsUiState
    data class Error(val message: String) : HouseSettingsUiState
}

sealed interface UpdateHouseSettingsUiState {
    data object Idle : UpdateHouseSettingsUiState
    data object Loading : UpdateHouseSettingsUiState
    data object Success : UpdateHouseSettingsUiState
    data class Error(val message: String) : UpdateHouseSettingsUiState
}


