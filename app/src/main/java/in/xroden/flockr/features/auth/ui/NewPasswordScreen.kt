/** Choosing a new password after opening the reset link from the email. */
package `in`.xroden.flockr.features.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import `in`.xroden.flockr.features.auth.presentation.AuthValidation
import `in`.xroden.flockr.features.auth.presentation.PasswordResetState
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/** Covers the app until a new password is saved; the reset link has already signed the user in. */
@Composable
fun NewPasswordScreen(state: PasswordResetState, onSave: (String) -> Unit) {
    val haptics = rememberHaptics()
    var password by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val isSaving = state == PasswordResetState.Saving
    val error = AuthValidation.newPasswordError(password).takeIf { showErrors } ?: (state as? PasswordResetState.ChoosingPassword)?.error

    fun submit() {
        showErrors = true
        if (AuthValidation.newPasswordError(password) != null) haptics.error() else onSave(password)
    }

    Scaffold(
        bottomBar = { FormSubmitBar(text = "Save password", onClick = ::submit, enabled = password.isNotEmpty() && !isSaving, isLoading = isSaving) },
    ) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero("Reset password") {
                    HeroNote("Choose the password you'll sign in with from now on.")
                }
            },
        ) {
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "New password",
                autofill = ContentType.NewPassword,
                error = error,
                enabled = !isSaving,
                onDone = ::submit,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}
