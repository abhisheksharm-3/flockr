/** Signing in with an email and password, or with Google. */
package `in`.xroden.flockr.features.auth.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
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
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

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
        topBar = { FlockrTopAppBar(title = "Sign in", subtitle = "Welcome back to Flockr", onNavigateBack = onNavigateBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
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
            FlockrPrimaryButton(
                text = "Sign in",
                onClick = ::submit,
                enabled = !isBusy && email.isNotBlank() && password.isNotEmpty(),
                isLoading = loading?.withGoogle == false,
                modifier = Modifier.fillMaxWidth(),
            )
            OrDivider()
            GoogleSignInButton(
                onClick = { activity?.let(viewModel::signInWithGoogle) },
                enabled = !isBusy && activity != null,
                isLoading = loading?.withGoogle == true,
            )
            TextButton(onClick = onNavigateToSignup, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("New to Flockr? Create an account")
            }
        }
    }
}
