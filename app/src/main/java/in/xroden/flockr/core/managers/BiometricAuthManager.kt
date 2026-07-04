package `in`.xroden.flockr.core.managers

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Manages biometric authentication using the AndroidX Biometric library. */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricManager = BiometricManager.from(context)

    /** Returns true if biometric or device credential authentication is available. */
    fun canAuthenticate(): Boolean =
        biometricManager.canAuthenticate(allowedAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows biometric authentication prompt.
     *
     * @param activity The FragmentActivity to show the prompt
     * @param title The title text for the prompt
     * @param subtitle The subtitle text for the prompt
     * @param onSuccess Called when authentication succeeds
     * @param onError Called when authentication fails with error message
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Flockr Locked",
        subtitle: String = "Unlock to access your finances",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }

        val authenticators = allowedAuthenticators()
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply {
                // Below API 30 the STRONG|DEVICE_CREDENTIAL combination is unsupported and a
                // device-credential prompt has no negative button, so build() would throw.
                if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                    setNegativeButtonText("Cancel")
                }
            }
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    private fun allowedAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
}
