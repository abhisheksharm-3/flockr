/** The one step after a first sign-in: confirming the name housemates will see. */
package `in`.xroden.flockr.features.auth.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.auth.presentation.AuthValidation
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * Finishing marks onboarding complete on the profile, and the app moves on when the auth state
 * sees it, so [onComplete] is not needed to leave this screen. [viewModel] defaults to the
 * activity's instance, the one the app's navigation reads, so the finished profile reaches it.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(checkNotNull(LocalActivity.current as? ComponentActivity))
) {
    val haptics = rememberHaptics()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isSaving by viewModel.isUpdatingProfile.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val draftName = name ?: profile?.fullName.orEmpty()
    val nameError = AuthValidation.nameError(draftName).takeIf { showErrors }

    LaunchedEffect(actionError) {
        val message = actionError ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.clearActionError()
    }

    fun finish() {
        showErrors = true
        if (AuthValidation.nameError(draftName) != null) {
            haptics.error()
            return
        }
        viewModel.updateProfile(fullName = draftName.trim(), hasCompletedOnboarding = true)
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Welcome to Flockr", subtitle = "One thing before you start", onNavigateBack = null) },
        bottomBar = {
            FlockrPrimaryButton(
                text = "Continue",
                onClick = ::finish,
                enabled = draftName.isNotBlank(),
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "What should your housemates call you? You can change it later in settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthTextField(
                value = draftName,
                onValueChange = { name = it },
                label = "Name",
                leadingIcon = Icons.Rounded.Person,
                autofill = ContentType.PersonFullName,
                capitalization = KeyboardCapitalization.Words,
                error = nameError,
                enabled = !isSaving,
                onDone = ::finish,
            )
        }
    }
}
