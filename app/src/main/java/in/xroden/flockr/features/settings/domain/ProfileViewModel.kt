package `in`.xroden.flockr.features.settings.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateProfileUiState>(UpdateProfileUiState.Idle)
    val updateState: StateFlow<UpdateProfileUiState> = _updateState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            authRepository.getProfile().fold(
                onSuccess = { profile ->
                    if (profile != null) {
                        _uiState.value = ProfileUiState.Success(profile)
                    } else {
                        _uiState.value = ProfileUiState.Error("Profile not found")
                    }
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(
                        message = error.message ?: "Failed to load profile"
                    )
                }
            )
        }
    }

    fun updateProfile(fullName: String) {
        if (fullName.isBlank()) {
            _updateState.value = UpdateProfileUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _updateState.value = UpdateProfileUiState.Loading
            
            authRepository.updateProfile(fullName = fullName, hasCompletedOnboarding = null).fold(
                onSuccess = {
                    _updateState.value = UpdateProfileUiState.Success
                    // Reload profile to get updated data
                    loadProfile()
                    kotlinx.coroutines.delay(1000)
                    _updateState.value = UpdateProfileUiState.Idle
                },
                onFailure = { error ->
                    _updateState.value = UpdateProfileUiState.Error(
                        message = error.message ?: "Failed to update profile"
                    )
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileUiState.Idle
    }

    fun uploadProfilePicture(imageData: ByteArray) {
        // TODO: Implement profile picture upload when storage is configured
        _updateState.value = UpdateProfileUiState.Error("Profile picture upload not yet implemented")
    }
}
