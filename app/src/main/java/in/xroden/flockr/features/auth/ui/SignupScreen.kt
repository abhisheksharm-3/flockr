/** Creating an account with a name, email and password, or with Google. */
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
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.auth.presentation.AuthValidation
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.auth.presentation.SignInUiState
import `in`.xroden.flockr.features.auth.presentation.SignUpUiState
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val activity = LocalActivity.current
    val signUpState by viewModel.signUpState.collectAsStateWithLifecycle()
    val signInState by viewModel.signInState.collectAsStateWithLifecycle()
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val isSigningUp = signUpState is SignUpUiState.Loading
    val isSigningInWithGoogle = signInState is SignInUiState.Loading
    val isBusy = isSigningUp || isSigningInWithGoogle
    val serverError = (signUpState as? SignUpUiState.Error)?.message ?: (signInState as? SignInUiState.Error)?.message

    val nameError = AuthValidation.nameError(fullName).takeIf { showErrors }
    val emailError = AuthValidation.emailError(email).takeIf { showErrors }
    val passwordError = AuthValidation.newPasswordError(password).takeIf { showErrors }
    val confirmationError = AuthValidation.confirmationError(password, confirmation).takeIf { showErrors || confirmation.isNotEmpty() }

    LaunchedEffect(serverError) {
        if (serverError != null) haptics.error()
    }

    fun submit() {
        showErrors = true
        val hasError = listOf(
            AuthValidation.nameError(fullName),
            AuthValidation.emailError(email),
            AuthValidation.newPasswordError(password),
            AuthValidation.confirmationError(password, confirmation),
        ).any { it != null }
        if (hasError) {
            haptics.error()
            return
        }
        viewModel.signUp(email.trim(), password, fullName.trim())
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Create account", subtitle = "Share a house without the spreadsheets", onNavigateBack = onNavigateToLogin) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AuthTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Name",
                leadingIcon = Icons.Rounded.Person,
                autofill = ContentType.PersonFullName,
                capitalization = KeyboardCapitalization.Words,
                error = nameError,
                supportingText = "How your housemates see you",
                enabled = !isBusy,
            )
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
                autofill = ContentType.NewPassword,
                error = passwordError,
                supportingText = "At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters",
                enabled = !isBusy,
            )
            PasswordField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirm password",
                autofill = ContentType.NewPassword,
                error = confirmationError,
                enabled = !isBusy,
                onDone = ::submit,
            )
            serverError?.let { AuthErrorMessage(it) }
            FlockrPrimaryButton(
                text = "Create account",
                onClick = ::submit,
                enabled = !isBusy && fullName.isNotBlank() && email.isNotBlank() && password.isNotEmpty() && confirmation.isNotEmpty(),
                isLoading = isSigningUp,
                modifier = Modifier.fillMaxWidth(),
            )
            OrDivider()
            GoogleSignInButton(
                onClick = { activity?.let(viewModel::signInWithGoogle) },
                enabled = !isBusy && activity != null,
                isLoading = isSigningInWithGoogle,
            )
            TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already have an account? Sign in")
            }
        }
    }
}
