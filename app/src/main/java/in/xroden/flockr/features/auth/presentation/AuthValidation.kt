/** The checks the sign-in and sign-up forms run before asking the auth server. */
package `in`.xroden.flockr.features.auth.presentation

import android.util.Patterns

/** Each check returns the line to show under the field, or null when the value is fine. */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    fun emailError(email: String): String? =
        if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) null else "Enter a valid email address"

    fun passwordError(password: String): String? =
        if (password.isEmpty()) "Enter your password" else null

    fun newPasswordError(password: String): String? =
        if (password.length >= MIN_PASSWORD_LENGTH) null else "Use at least $MIN_PASSWORD_LENGTH characters"

    fun confirmationError(password: String, confirmation: String): String? =
        if (password == confirmation) null else "Passwords don't match"

    fun nameError(name: String): String? =
        if (name.isBlank()) "Enter your name" else null
}
