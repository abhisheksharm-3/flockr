package `in`.xroden.flockr.features.auth.data

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for native Google Sign-In using Credential Manager.
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Initiates native Google Sign-In flow and returns the ID token.
     */
    suspend fun signIn(activity: Activity): Result<String> = runCatching {
        val credentialManager = CredentialManager.create(context)
        
        val googleClientId = BuildConfig.GOOGLE_CLIENT_ID

        if (googleClientId.isBlank()) {
            throw IllegalStateException(
                "GOOGLE_CLIENT_ID not configured. Add GOOGLE_CLIENT_ID=your-client-id to local.properties"
            )
        }
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(googleClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            
            when (credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        googleIdTokenCredential.idToken
                    } else {
                        throw IllegalStateException("Unexpected credential type: ${credential.type}")
                    }
                }
                else -> {
                    throw IllegalStateException("Unexpected credential type received")
                }
            }
        } catch (e: GetCredentialCancellationException) {
            throw Exception("Sign-in cancelled")
        } catch (e: NoCredentialException) {
            throw Exception("No Google accounts available. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            throw Exception("Sign-in failed: ${e.errorMessage ?: e.message}")
        }
    }
}
