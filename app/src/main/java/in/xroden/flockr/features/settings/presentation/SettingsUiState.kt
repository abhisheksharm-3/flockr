/** What the settings and profile screens show, and the one-off outcomes of changing them. */
package `in`.xroden.flockr.features.settings.presentation

import `in`.xroden.flockr.features.auth.model.Profile

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: Profile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/** Which profile change is in flight, so the screen can disable the controls it would clash with. */
sealed interface UpdateProfileUiState {
    data object Idle : UpdateProfileUiState
    data object Saving : UpdateProfileUiState
    data object UploadingPhoto : UpdateProfileUiState
}

sealed interface ProfileEvent {
    data object Saved : ProfileEvent
    data object PhotoChanged : ProfileEvent
    data class Failed(val message: String) : ProfileEvent
}

sealed interface AppLockEvent {
    data object Enabled : AppLockEvent
    data class Failed(val message: String) : AppLockEvent
}
