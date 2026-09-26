/** What the sign-in, sign-up and onboarding screens show while talking to the auth server. */
package `in`.xroden.flockr.features.auth.presentation

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

    /** [withGoogle] tells the screen which button to show the loading indicator on. */
    data class Loading(val withGoogle: Boolean) : SignInUiState
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

/** Where a forgotten password stands: asking for the email, the email on its way, then choosing a new one. */
sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Sending : PasswordResetState
    data class EmailSent(val email: String) : PasswordResetState
    data class Failed(val message: String) : PasswordResetState

    /** Opened from the emailed link and signed in; [error] is why the last new password was refused. */
    data class ChoosingPassword(val error: String? = null) : PasswordResetState

    data object Saving : PasswordResetState
}

sealed interface AccountDeletionState {
    data object Idle : AccountDeletionState
    data object Deleting : AccountDeletionState
    data class Failed(val message: String) : AccountDeletionState
}
