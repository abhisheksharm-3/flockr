package `in`.xroden.flockr.features.auth.domain

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.auth.model.Profile

@Immutable
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object NotAuthenticated : AuthUiState
    data class Authenticated(val profile: Profile) : AuthUiState
    data class Error(val message: String, val cause: Throwable? = null) : AuthUiState
}

@Immutable
sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Loading : SignInUiState
    data object Success : SignInUiState
    data class Error(val message: String) : SignInUiState
}

@Immutable
sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Loading : SignUpUiState
    data object Success : SignUpUiState
    data class Error(val message: String) : SignUpUiState
}


