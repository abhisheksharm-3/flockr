package `in`.xroden.flockr.features.auth.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.auth.model.Profile
import android.util.Log
import `in`.xroden.flockr.features.auth.data.AuthRepository
import `in`.xroden.flockr.ui.navigation.AuthNavigationState
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Wrap the repository flow so we can enforce a timeout/fallback for LoadingFromStorage
    private val _sessionState = MutableStateFlow<SessionStatus>(SessionStatus.LoadingFromStorage)
    val sessionStatus: StateFlow<SessionStatus> = _sessionState.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Combined state for navigation decisions
    val authNavigationState: StateFlow<AuthNavigationState> =
        combine(
            sessionStatus,
            _profile
        ) { session, profile ->
            when (session) {
                // Still loading session from storage
                is SessionStatus.LoadingFromStorage ->
                    AuthNavigationState.Loading

                // Not authenticated - show login screen
                is SessionStatus.NotAuthenticated ->
                    AuthNavigationState.Unauthenticated

                // Network error - treat as not authenticated
                is SessionStatus.NetworkError ->
                    AuthNavigationState.Unauthenticated

                // Authenticated - check profile and onboarding status
                is SessionStatus.Authenticated -> {
                    when {
                        // Profile not loaded yet
                        profile == null ->
                            AuthNavigationState.Loading

                        // Profile loaded but onboarding not complete
                        profile.hasCompletedOnboarding == false ->
                            AuthNavigationState.NeedsOnboarding

                        // Fully authenticated and onboarded
                        else ->
                            AuthNavigationState.Authenticated
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthNavigationState.Loading
        )

    init {
        Log.d("AuthViewModel", "========== Initializing AuthViewModel ==========")
        Log.d("AuthViewModel", "Initial session state: ${_sessionState.value::class.simpleName}")
        Log.d("AuthViewModel", "Initial profile: ${_profile.value}")

        // Start a global timeout to prevent infinite loading
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting timeout timer (3 seconds)")
            kotlinx.coroutines.delay(3000) // 3 second timeout
            val currentState = _sessionState.value
            Log.d("AuthViewModel", "Timeout reached. Current state: ${currentState::class.simpleName}")
            if (currentState is SessionStatus.LoadingFromStorage) {
                Log.w("AuthViewModel", "⚠️ TIMEOUT: Still in LoadingFromStorage after 3s, forcing NotAuthenticated")
                _sessionState.value = SessionStatus.NotAuthenticated(false)
            } else {
                Log.d("AuthViewModel", "Timeout check passed - state already changed to: ${currentState::class.simpleName}")
            }
        }

        // Monitor authNavigationState changes
        viewModelScope.launch {
            authNavigationState.collect { navState ->
                Log.d("AuthViewModel", ">>> Navigation state changed to: ${navState::class.simpleName}")
            }
        }

        // Collect repository session flow, mirror into _sessionState
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting to collect session flow from repository")
            try {
                authRepository.sessionFlow.collect { status ->
                    Log.d("AuthViewModel", "📨 Session status received: ${status::class.simpleName}")
                    when (status) {
                        is SessionStatus.LoadingFromStorage -> {
                            Log.d("AuthViewModel", "Setting state to LoadingFromStorage")
                            _sessionState.value = status
                        }
                        is SessionStatus.Authenticated -> {
                            Log.d("AuthViewModel", "✅ User authenticated, loading profile...")
                            _sessionState.value = status
                            // Load profile when authenticated
                            loadProfile()
                        }
                        is SessionStatus.NotAuthenticated -> {
            Log.d("AuthViewModel", "🔄 Loading profile...")
            try {
                val profile = authRepository.getProfile()
                _profile.value = profile
                Log.d("AuthViewModel", "Profile loaded: $profile")
                if (profile != null) {
                    Log.d("AuthViewModel", "Profile details - Name: ${profile.fullName}, Onboarded: ${profile.hasCompletedOnboarding}")
                } else {
                    Log.w("AuthViewModel", "Profile is null!")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading profile", e)
                _profile.value = null
            }
                            _sessionState.value = status
                            _profile.value = null
                        }
                        is SessionStatus.NetworkError -> {
                            Log.e("AuthViewModel", "🌐 Network error occurred, treating as not authenticated")
                            // Treat network error as not authenticated
                            _sessionState.value = SessionStatus.NotAuthenticated(false)
                            _profile.value = null
                        }
                    }
                    Log.d("AuthViewModel", "Current _sessionState: ${_sessionState.value::class.simpleName}")
                    Log.d("AuthViewModel", "Current _profile: ${_profile.value}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "💥 Error collecting session flow", e)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = authRepository.getProfile()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email, password).fold(
                onSuccess = {
                    loadProfile()
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                }
            )
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUp(email, password, fullName).fold(
                onSuccess = {
                    loadProfile()
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign up failed")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _profile.value = null
        }
    }

    suspend fun updateProfile(fullName: String? = null, hasCompletedOnboarding: Boolean? = null): Result<Unit> {
        Log.d("AuthViewModel", "updateProfile called - fullName=$fullName, hasCompletedOnboarding=$hasCompletedOnboarding")
        return authRepository.updateProfile(fullName, hasCompletedOnboarding).also { result ->
            result.fold(
                onSuccess = {
                    Log.d("AuthViewModel", "Profile update successful, reloading profile...")
                    loadProfile()
                },
                onFailure = { error ->
                    Log.e("AuthViewModel", "Profile update failed", error)
                }
            )
        }
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
