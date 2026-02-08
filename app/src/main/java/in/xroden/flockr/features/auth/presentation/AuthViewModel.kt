package `in`.xroden.flockr.features.auth.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.auth.data.IAuthRepository
import `in`.xroden.flockr.features.auth.data.GoogleSignInHelper
import `in`.xroden.flockr.ui.navigation.state.AuthNavigationState
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication state management.
 * Handles sign-in, sign-up, and session state.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    val sessionStatus: StateFlow<SessionStatus> = _sessionState.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.NotAuthenticated)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _signInState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val signInState: StateFlow<SignInUiState> = _signInState.asStateFlow()

    private val _signUpState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    val profile: StateFlow<`in`.xroden.flockr.features.auth.model.Profile?> =
        _uiState.map { state ->
            when (state) {
                is AuthUiState.Authenticated -> state.profile
                else -> null
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val authNavigationState: StateFlow<AuthNavigationState> =
        combine(sessionStatus, _uiState) { session, state ->
            when (session) {
                is SessionStatus.Initializing -> AuthNavigationState.Loading
                is SessionStatus.NotAuthenticated -> AuthNavigationState.Unauthenticated
                is SessionStatus.RefreshFailure -> AuthNavigationState.Unauthenticated
                is SessionStatus.Authenticated -> {
                    when (state) {
                        is AuthUiState.Loading -> AuthNavigationState.Loading
                        is AuthUiState.Authenticated -> {
                            if (state.profile.hasCompletedOnboarding) {
                                AuthNavigationState.Authenticated
                            } else {
                                AuthNavigationState.NeedsOnboarding
                            }
                        }
                        is AuthUiState.Error, is AuthUiState.NotAuthenticated -> {
                            AuthNavigationState.Unauthenticated
                        }
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthNavigationState.Loading
        )

    init {
        observeSessionFlow()
    }

    private fun observeSessionFlow() {
        viewModelScope.launch {
            authRepository.sessionFlow.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        _sessionState.value = status
                        _uiState.value = AuthUiState.Loading
                    }
                    is SessionStatus.Authenticated -> {
                        _sessionState.value = status
                        loadProfile()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _sessionState.value = status
                        _uiState.value = AuthUiState.NotAuthenticated
                    }
                    is SessionStatus.RefreshFailure -> {
                        _sessionState.value = SessionStatus.NotAuthenticated(false)
                        _uiState.value = AuthUiState.NotAuthenticated
                    }
                }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getProfile().fold(
                onSuccess = { profile ->
                    if (profile != null) {
                        _uiState.value = AuthUiState.Authenticated(profile)
                    } else {
                        _uiState.value = AuthUiState.Error("Profile not found", null)
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        message = error.message ?: "Failed to load profile",
                        cause = error
                    )
                }
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _signInState.value = SignInUiState.Loading

            authRepository.signIn(email, password).fold(
                onSuccess = {
                    _signInState.value = SignInUiState.Success
                    delay(500)
                    _signInState.value = SignInUiState.Idle
                },
                onFailure = { error ->
                    _signInState.value = SignInUiState.Error(
                        message = error.message ?: "Sign in failed"
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _signUpState.value = SignUpUiState.Loading

            authRepository.signUp(email, password, fullName).fold(
                onSuccess = {
                    _signUpState.value = SignUpUiState.Success
                    delay(500)
                    _signUpState.value = SignUpUiState.Idle
                },
                onFailure = { error ->
                    _signUpState.value = SignUpUiState.Error(
                        message = error.message ?: "Sign up failed"
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.NotAuthenticated
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _signInState.value = SignInUiState.Loading

            googleSignInHelper.signIn(activity).fold(
                onSuccess = { idToken ->
                    authRepository.signInWithGoogleIdToken(idToken).fold(
                        onSuccess = {
                            _signInState.value = SignInUiState.Success
                            delay(500)
                            _signInState.value = SignInUiState.Idle
                        },
                        onFailure = { error ->
                            _signInState.value = SignInUiState.Error(
                                message = error.message ?: "Failed to authenticate with Supabase"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _signInState.value = SignInUiState.Error(
                        message = error.message ?: "Google sign in failed"
                    )
                }
            )
        }
    }

    fun updateProfile(fullName: String? = null, hasCompletedOnboarding: Boolean? = null) {
        viewModelScope.launch {
            authRepository.updateProfile(
                fullName = fullName,
                avatarUrl = null,
                hasCompletedOnboarding = hasCompletedOnboarding
            ).fold(
                onSuccess = { loadProfile() },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        message = error.message ?: "Failed to update profile",
                        cause = error
                    )
                }
            )
        }
    }

    fun resetSignInState() {
        _signInState.value = SignInUiState.Idle
    }

    fun resetSignUpState() {
        _signUpState.value = SignUpUiState.Idle
    }
}
