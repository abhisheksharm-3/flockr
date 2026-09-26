/** The signed-in user's profile: loading it, renaming it and replacing its photo. */
package `in`.xroden.flockr.features.settings.presentation

import android.content.Context
import `in`.xroden.flockr.utils.uploadJpegOf
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.features.auth.data.AuthRepository
import `in`.xroden.flockr.core.validation.Validators
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AVATAR_BUCKET = "avatars"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateProfileUiState>(UpdateProfileUiState.Idle)
    val updateState: StateFlow<UpdateProfileUiState> = _updateState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch { refresh() }
    }

    /** Once a profile is showing, reloads it in place and keeps it if the reload fails. */
    private suspend fun refresh() {
        if (_uiState.value !is ProfileUiState.Success) _uiState.value = ProfileUiState.Loading
        authRepository.getProfile().fold(
            onSuccess = { profile ->
                _uiState.value = profile?.let { ProfileUiState.Success(it) }
                    ?: ProfileUiState.Error("We couldn't find your profile. Try again.")
            },
            onFailure = { error ->
                if (_uiState.value !is ProfileUiState.Success) _uiState.value = ProfileUiState.Error(error.userMessage())
            }
        )
    }

    /** Saves whichever of the name and UPI ID changed; a blank UPI ID removes it. */
    fun updateProfile(fullName: String, upiId: String) {
        if (_updateState.value != UpdateProfileUiState.Idle) return
        val current = (_uiState.value as? ProfileUiState.Success)?.profile ?: return
        val name = fullName.trim()
        if (name.isEmpty()) {
            _events.trySend(ProfileEvent.Failed("Enter your name."))
            return
        }
        val upi = Validators.validateUpiId(upiId).getOrElse {
            _events.trySend(ProfileEvent.Failed(it.userMessage()))
            return
        }
        _updateState.value = UpdateProfileUiState.Saving
        viewModelScope.launch {
            runCatching {
                if (name != current.fullName) authRepository.updateProfile(fullName = name).getOrThrow()
                if (upi != current.upiId) authRepository.updateUpiId(upi).getOrThrow()
            }.fold(
                onSuccess = {
                    refresh()
                    _events.send(ProfileEvent.Saved)
                },
                onFailure = { error -> _events.send(ProfileEvent.Failed(error.userMessage())) }
            )
            _updateState.value = UpdateProfileUiState.Idle
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileUiState.Idle
    }

    /** Reads, compresses and uploads the picked image entirely off the main thread. */
    fun uploadProfilePicture(uri: Uri, context: Context) {
        val resolver = context.contentResolver
        uploadPhoto { resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read image") }
    }

    fun uploadProfilePicture(imageData: ByteArray) {
        uploadPhoto { imageData }
    }

    /** [readImage] runs on the IO dispatcher, alongside the compression. */
    private fun uploadPhoto(readImage: () -> ByteArray) {
        if (_updateState.value != UpdateProfileUiState.Idle) return
        val userId = authRepository.currentUser?.id
        if (userId == null) {
            _events.trySend(ProfileEvent.Failed("Sign in again to change your photo."))
            return
        }
        _updateState.value = UpdateProfileUiState.UploadingPhoto
        viewModelScope.launch {
            runCatching {
                val compressed = withContext(Dispatchers.IO) { uploadJpegOf(readImage()) }
                    ?: error("That image couldn't be read. Try another.")
                val fileName = "$userId/avatar_${System.currentTimeMillis()}.jpg"
                val publicUrl = storageRepository.uploadFile(AVATAR_BUCKET, fileName, compressed).getOrThrow()
                authRepository.updateProfile(fullName = null, hasCompletedOnboarding = null, avatarUrl = publicUrl).getOrThrow()
            }.fold(
                onSuccess = {
                    refresh()
                    _events.send(ProfileEvent.PhotoChanged)
                },
                onFailure = { error -> _events.send(ProfileEvent.Failed(error.userMessage())) }
            )
            _updateState.value = UpdateProfileUiState.Idle
        }
    }
}
