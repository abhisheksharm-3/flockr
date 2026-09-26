/** Signing in with Google, or with an email and password. */
package `in`.xroden.flockr.features.auth.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.auth.presentation.AuthUiState
import `in`.xroden.flockr.features.auth.presentation.AuthValidation
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.auth.presentation.SignInUiState
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/** A cobalt welcome, then Google because it is one tap, then the email fields under their own heading, with the sign-in button pinned where the thumb is. */
@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val activity = LocalActivity.current
    val signInState by viewModel.signInState.collectAsStateWithLifecycle()
    val authState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val loading = signInState as? SignInUiState.Loading
    val isBusy = loading != null
    val emailError = AuthValidation.emailError(email).takeIf { showErrors }
    val passwordError = AuthValidation.passwordError(password).takeIf { showErrors }
    val serverError = (signInState as? SignInUiState.Error)?.message ?: (authState as? AuthUiState.Error)?.message

    LaunchedEffect(serverError) {
        if (serverError != null) haptics.error()
    }

    fun submit() {
        showErrors = true
        if (AuthValidation.emailError(email) != null || AuthValidation.passwordError(password) != null) {
            haptics.error()
            return
        }
        viewModel.signIn(email.trim(), password)
    }

    Scaffold(
        bottomBar = {
            FormSubmitBar(
                text = "Sign in",
                onClick = ::submit,
                enabled = !isBusy && email.isNotBlank() && password.isNotEmpty(),
                isLoading = loading?.withGoogle == false,
            )
        },
    ) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero("Sign in") {
                    Text("Welcome back", style = MaterialTheme.typography.displaySmallEmphasized)
                    HeroNote("Your houses are right where you left them.")
                }
            },
        ) {
            GoogleSignInButton(
                onClick = { activity?.let(viewModel::signInWithGoogle) },
                enabled = !isBusy && activity != null,
                isLoading = loading?.withGoogle == true,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            Column {
                SectionTitle("Or use your email")
                Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        leadingIcon = Icons.Rounded.Email,
                        autofill = ContentType.EmailAddress,
                        keyboardType = KeyboardType.Email,
                        error = emailError,
                        enabled = !isBusy,
                    )
                    PasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        autofill = ContentType.Password,
                        error = passwordError,
                        enabled = !isBusy,
                        onDone = ::submit,
                    )
                    serverError?.let { AuthErrorMessage(it) }
                }
            }
            TextButton(
                onClick = onNavigateToSignup,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Spacing.lg),
            ) {
                Text("New to Flockr? Create an account", style = MaterialTheme.typography.titleSmallEmphasized)
            }
        }
    }
}
