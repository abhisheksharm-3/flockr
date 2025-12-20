package `in`.xroden.flockr.features.auth.data

import android.app.Activity
import android.content.Context
import android.util.Log
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
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleSignIn"
    }
    
    /**
     * Initiates native Google Sign-In flow and returns the ID token.
     */
    suspend fun signIn(activity: Activity): Result<String> = runCatching {
        Log.d(TAG, "Starting Google Sign-In flow")
        
        val credentialManager = CredentialManager.create(context)
        
        val googleClientId = BuildConfig.GOOGLE_CLIENT_ID
        Log.d(TAG, "Using Google Client ID: ${googleClientId.take(20)}...")
        
        if (googleClientId.isBlank()) {
            Log.e(TAG, "GOOGLE_CLIENT_ID is blank!")
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
        
        Log.d(TAG, "Requesting credentials from CredentialManager...")
        
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            
            Log.d(TAG, "Got credential of type: ${credential.javaClass.simpleName}")
            
            when (credential) {
                is CustomCredential -> {
                    Log.d(TAG, "CustomCredential type: ${credential.type}")
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Successfully obtained Google ID token")
                        googleIdTokenCredential.idToken
                    } else {
                        Log.e(TAG, "Unexpected credential type: ${credential.type}")
                        throw IllegalStateException("Unexpected credential type: ${credential.type}")
                    }
                }
                else -> {
                    Log.e(TAG, "Unexpected credential class: ${credential.javaClass.name}")
                    throw IllegalStateException("Unexpected credential type received")
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Sign-in cancelled by user")
            throw Exception("Sign-in cancelled")
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException: ${e.message}", e)
            throw Exception("No Google accounts available. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.errorMessage}", e)
            Log.e(TAG, "Exception type: ${e.type}")
            throw Exception("Sign-in failed: ${e.errorMessage ?: e.message}")
        }
    }.also { result ->
        result.onFailure { error ->
            Log.e(TAG, "Google Sign-In failed", error)
        }
        result.onSuccess {
            Log.d(TAG, "Google Sign-In succeeded")
        }
    }
}
