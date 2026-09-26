/** Changing your name, profile photo and the UPI ID housemates pay you at. */
package `in`.xroden.flockr.features.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.settings.presentation.ProfileEvent
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.UpdateProfileUiState
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/** Your photo and name on cobalt, as housemates will see them, the name typed large; the email you sign in with follows. */
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val profile = (profileState as? ProfileUiState.Success)?.profile
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var upi by rememberSaveable { mutableStateOf<String?>(null) }
    val draftName = name ?: profile?.fullName.orEmpty()
    val draftUpi = upi ?: profile?.upiId.orEmpty()
    val isBusy = updateState != UpdateProfileUiState.Idle

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadProfilePicture(it, context) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.Saved -> {
                    haptics.success()
                    onNavigateBack()
                }
                ProfileEvent.PhotoChanged -> {
                    haptics.success()
                    snackbarHostState.showSnackbar("Photo updated")
                }
                is ProfileEvent.Failed -> {
                    haptics.error()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val isSaving = updateState == UpdateProfileUiState.Saving
    val isUploading = updateState == UpdateProfileUiState.UploadingPhoto
    val nameMissing = name != null && draftName.isBlank()
    val pickPhoto = { haptics.tap(); photoPicker.launch("image/*") }

    Scaffold(
        bottomBar = {
            if (profile != null) {
                FormSubmitBar(
                    text = "Save changes",
                    onClick = { viewModel.updateProfile(draftName, draftUpi) },
                    enabled = draftName.isNotBlank() && !isBusy &&
                        (draftName.trim() != profile.fullName || draftUpi.trim() != profile.upiId.orEmpty()),
                    isLoading = isSaving,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = profileState) {
            ProfileUiState.Loading -> SkeletonFormScreen()
            is ProfileUiState.Error -> ErrorState(current.message, modifier = Modifier.padding(padding), onRetry = viewModel::loadProfile)
            is ProfileUiState.Success -> HeroColumn(
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                hero = {
                    FormHero("Your profile") {
                        MemberAvatar(
                            name = draftName,
                            avatarUrl = current.profile.avatarUrl,
                            size = IconSize.display,
                            modifier = Modifier
                                .padding(vertical = Spacing.sm)
                                .clip(CircleShape)
                                .clickable(enabled = !isBusy, onClickLabel = "Change photo", role = Role.Button) { pickPhoto() },
                        )
                        HeroTextInput(
                            value = draftName,
                            onValueChange = { name = it },
                            placeholder = "Your name",
                            enabled = !isSaving,
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                        )
                        HeroNote(if (nameMissing) "Enter your name" else "How your housemates see you", isError = nameMissing)
                    }
                },
            ) {
                Sentence {
                    SentenceToken(
                        text = when {
                            isUploading -> "Uploading…"
                            current.profile.avatarUrl == null -> "Add a photo"
                            else -> "Change photo"
                        },
                        onClick = { photoPicker.launch("image/*") },
                        enabled = !isBusy,
                        icon = Icons.Rounded.AddAPhoto,
                        isUnset = current.profile.avatarUrl == null,
                    )
                }
                FlockrTextField(
                    value = draftUpi,
                    onValueChange = { upi = it },
                    label = "UPI ID",
                    placeholder = "name@bank",
                    keyboardType = KeyboardType.Email,
                    enabled = !isSaving,
                    supportingText = "So housemates can pay you straight from Flockr",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                )
                FlockrTextField(
                    value = current.profile.email,
                    onValueChange = {},
                    label = "Email",
                    readOnly = true,
                    enabled = false,
                    supportingText = "The address you sign in with",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                )
            }
        }
    }
}
