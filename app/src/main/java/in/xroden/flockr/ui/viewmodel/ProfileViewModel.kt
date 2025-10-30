package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Profile
import `in`.xroden.flockr.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = authRepository.getProfile()
        }
    }

    fun updateProfileName(fullName: String) {
        viewModelScope.launch {
            authRepository.updateProfile(fullName = fullName, hasCompletedOnboarding = null)
            loadProfile()
        }
    }

    fun uploadProfilePicture(imageData: ByteArray, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser?.id ?: return@launch
                val fileName = "avatar_$userId.jpg"

                // Upload to storage
                supabase.storage.from("avatars").upload(fileName, imageData, upsert = true)

                // Get public URL
                val url = supabase.storage.from("avatars").publicUrl(fileName)

                // Update profile with image URL
                // You would need to add an avatar_url column to the profiles table

                onSuccess(url)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

