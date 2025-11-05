package `in`.xroden.flockr.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Profile
import `in`.xroden.flockr.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "loadProfile: Loading user profile")

            try {
                val profile = authRepository.getProfile()
                if (profile != null) {
                    Log.d(TAG, "loadProfile: Success - ${profile.fullName}")
                    _profile.value = profile
                } else {
                    Log.e(TAG, "loadProfile: Profile is null")
                    _error.value = "Failed to load profile"
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadProfile: Failed", e)
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(fullName: String) {
        if (fullName.isBlank()) {
            _error.value = "Name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false

            Log.d(TAG, "updateProfile: Updating profile with name=$fullName")

            val result = authRepository.updateProfile(fullName = fullName, hasCompletedOnboarding = null)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "updateProfile: Success")
                    _updateSuccess.value = true
                    _isLoading.value = false
                    // Reload profile to get updated data
                    loadProfile()
                },
                onFailure = { e ->
                    Log.e(TAG, "updateProfile: Failed", e)
                    _error.value = e.message ?: "Failed to update profile"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun updateProfileName(fullName: String) {
        updateProfile(fullName)
    }

    fun uploadProfilePicture(imageData: ByteArray, onSuccess: (String) -> Unit) {
        // TODO: Implement profile picture upload when storage is configured
        Log.d(TAG, "uploadProfilePicture: Not yet implemented")
        // For now, just log that this feature is not available
    }
}

