/** The one step after a first sign-in: confirming the name housemates will see. */
package `in`.xroden.flockr.features.auth.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.auth.presentation.AuthValidation
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * The question and its answer sit on the cobalt, the name typed large, with a row below showing how
 * it will read to housemates as it is typed.
 *
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
        bottomBar = {
            FormSubmitBar(
                text = "Start using Flockr",
                onClick = ::finish,
                enabled = draftName.isNotBlank(),
                isLoading = isSaving,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            hero = {
                FormHero("Welcome to Flockr") {
                    Text("What should your housemates call you?", style = MaterialTheme.typography.titleLargeEmphasized)
                    HeroTextInput(
                        value = draftName,
                        onValueChange = { name = it },
                        placeholder = "Your name",
                        enabled = !isSaving,
                        autoFocus = draftName.isEmpty(),
                        style = MaterialTheme.typography.displaySmallEmphasized,
                        modifier = Modifier.padding(top = Spacing.sm).semantics { contentType = ContentType.PersonFullName },
                    )
                    HeroNote(nameError ?: "It's how you show up on expenses, chores and chat. Change it any time in Settings.", isError = nameError != null)
                }
            },
        ) {
            SectionTitle("How housemates see you")
            ListRow(
                headline = draftName.trim().ifEmpty { "Your name" },
                supporting = "This is how you appear to the house",
                leading = { MemberAvatar(name = draftName, avatarUrl = profile?.avatarUrl) },
            )
        }
    }
}
