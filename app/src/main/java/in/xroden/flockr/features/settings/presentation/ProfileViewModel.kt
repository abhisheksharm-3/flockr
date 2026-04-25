package `in`.xroden.flockr.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.auth.data.IAuthRepository
import `in`.xroden.flockr.core.storage.IStorageRepository
import `in`.xroden.flockr.utils.BitmapUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val storageRepository: IStorageRepository,
    private val bitmapUtils: BitmapUtils
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
                    loadProfile()
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
        viewModelScope.launch {
            _updateState.value = UpdateProfileUiState.Loading
            
            val currentUser = authRepository.currentUser
            if (currentUser == null) {
                _updateState.value = UpdateProfileUiState.Error("User not logged in")
                return@launch
            }

            runCatching {
                val compressedBytes = bitmapUtils.compressImage(imageData)
                val fileName = "${currentUser.id}/avatar_${System.currentTimeMillis()}.jpg"
                val publicUrl = storageRepository.uploadFile("avatars", fileName, compressedBytes)
                    .getOrThrow()
                authRepository.updateProfile(
                    fullName = null,
                    hasCompletedOnboarding = null,
                    avatarUrl = publicUrl
                ).getOrThrow()
            }.fold(
                onSuccess = {
                    _updateState.value = UpdateProfileUiState.Success
                    _uiState.value = ProfileUiState.Loading
                    loadProfile()
                },
                onFailure = { error ->
                    _updateState.value = UpdateProfileUiState.Error(
                        message = error.message ?: "Failed to upload profile picture"
                    )
                }
            )
        }
    }
}
