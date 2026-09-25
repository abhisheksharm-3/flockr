/** Changing your name and profile photo. */
package `in`.xroden.flockr.features.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.settings.presentation.ProfileEvent
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.UpdateProfileUiState
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private val PhotoSize = 120.dp

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
    val draftName = name ?: profile?.fullName.orEmpty()
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

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Edit profile", onNavigateBack = onNavigateBack) },
        bottomBar = {
            if (profile != null) {
                FlockrPrimaryButton(
                    text = "Save",
                    onClick = { viewModel.updateProfile(draftName) },
                    enabled = draftName.isNotBlank() && draftName.trim() != profile.fullName && !isBusy,
                    isLoading = updateState == UpdateProfileUiState.Saving,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = profileState) {
            ProfileUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            is ProfileUiState.Error -> ErrorState(current.message, modifier = Modifier.padding(padding), onRetry = viewModel::loadProfile)
            is ProfileUiState.Success -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                MemberAvatar(name = draftName, avatarUrl = current.profile.avatarUrl, size = PhotoSize)
                FilledTonalButton(
                    onClick = { haptics.tap(); photoPicker.launch("image/*") },
                    enabled = !isBusy,
                ) {
                    if (updateState == UpdateProfileUiState.UploadingPhoto) {
                        LoadingIndicator(Modifier.size(IconSize.sm), color = LocalContentColor.current)
                    } else {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(IconSize.sm))
                    }
                    Text(if (current.profile.avatarUrl == null) "Add a photo" else "Change photo", modifier = Modifier.padding(start = Spacing.sm))
                }
                FormSectionCard(icon = Icons.Rounded.Person, title = "About you") {
                    FlockrTextField(
                        value = draftName,
                        onValueChange = { name = it },
                        label = "Name",
                        enabled = updateState != UpdateProfileUiState.Saving,
                        isError = name != null && draftName.isBlank(),
                        supportingText = if (name != null && draftName.isBlank()) "Enter your name" else "How your housemates see you",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlockrTextField(
                        value = current.profile.email,
                        onValueChange = {},
                        label = "Email",
                        readOnly = true,
                        enabled = false,
                        supportingText = "The address you sign in with",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
