package `in`.xroden.flockr.features.auth.data

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.handleDeeplinks
import android.content.Intent
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.realtime.OfflineCache
import `in`.xroden.flockr.core.realtime.cachedAs
import `in`.xroden.flockr.features.auth.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** The scheme and host the manifest routes to Flockr for a password-reset link; Supabase Auth allows it as a redirect. */
const val LINK_SCHEME = "flockr"
const val PASSWORD_RESET_HOST = "reset-password"
private const val PASSWORD_RESET_LINK = "$LINK_SCHEME://$PASSWORD_RESET_HOST"
private const val AVATARS_BUCKET = "avatars"
private const val PERSONAL_DOCUMENTS_BUCKET = "personal-documents"

/** Signing in and out, the password, the account and the signed-in user's profile, on Supabase Auth. */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    val sessionFlow: Flow<SessionStatus> = supabase.auth.sessionStatus

    val currentUser: UserInfo?
        get() = supabase.auth.currentUserOrNull()

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> = runCatching {
        supabase.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
            }
        }

        val newUserId = supabase.auth.currentUserOrNull()?.id
        if (newUserId != null) {
            supabase.from("profiles")
                .select(Columns.ALL) {
                    filter { eq("id", newUserId) }
                }
                .decodeSingle<Profile>()
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Sign in with a Google ID token obtained from Credential Manager.
     * Uses Supabase's IDToken provider for native authentication.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(IDToken) {
            this.provider = Google
            this.idToken = idToken
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    /** Emails [email] a link that opens Flockr to choose a new password. Unknown addresses get no email and no error. */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        supabase.auth.resetPasswordForEmail(email, redirectUrl = PASSWORD_RESET_LINK)
    }

    /**
     * Signs in from a password-reset link Flockr was opened with, and says whether [intent] was one.
     * The user is then signed in and should be asked for a new password.
     */
    fun openPasswordResetLink(intent: Intent): Boolean {
        val link = intent.data ?: return false
        if (link.scheme != LINK_SCHEME || link.host != PASSWORD_RESET_HOST) return false
        supabase.handleDeeplinks(intent)
        return true
    }

    suspend fun setNewPassword(password: String): Result<Unit> = runCatching {
        supabase.auth.updateUser { this.password = password }
    }

    /**
     * Deletes the account: the user's photo and personal files first, while they may still remove
     * them, then the account itself, which keeps a blank profile so housemates' ledgers still add up.
     * The session on this phone is dropped last, since the server has already ended it.
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val userId = requireAuthenticated(currentUser?.id)
        for (bucket in listOf(AVATARS_BUCKET, PERSONAL_DOCUMENTS_BUCKET)) {
            val files = supabase.storage.from(bucket).list(userId).map { "$userId/${it.name}" }
            if (files.isNotEmpty()) supabase.storage.from(bucket).delete(files)
        }
        supabase.postgrest.rpc("delete_my_account")
        supabase.auth.clearSession()
    }

    /** The signed-in user's profile, or the one saved last time when there is no network. */
    suspend fun getProfile(): Result<Profile?> {
        val userId = currentUser?.id ?: return Result.success(null)
        return runCatching {
            OfflineCache.fetchOrSaved(cachedAs<Profile>("profile_$userId")) {
                supabase.from("profiles").select(Columns.ALL) { filter { eq("id", userId) } }.decodeSingle<Profile>()
            }
        }
    }

    suspend fun updateProfile(fullName: String? = null, avatarUrl: String? = null, hasCompletedOnboarding: Boolean? = null): Result<Unit> = runCatching {
        val userId = currentUser?.id ?: throw IllegalStateException("No user logged in")

        if (fullName == null && hasCompletedOnboarding == null && avatarUrl == null) {
            return@runCatching
        }

        supabase.from("profiles")
            .update(
                ProfileUpdate(
                    fullName = fullName,
                    hasCompletedOnboarding = hasCompletedOnboarding,
                    avatarUrl = avatarUrl
                )
            ) {
                filter { eq("id", userId) }
            }
    }

    /** Sets the UPI ID housemates pay the user at, or clears it when [upiId] is null. */
    suspend fun updateUpiId(upiId: String?): Result<Unit> = runCatching {
        val userId = requireAuthenticated(currentUser?.id)
        supabase.from("profiles").update({ set("upi_id", upiId) }) { filter { eq("id", userId) } }
    }

    fun getCurrentUserId(): String? = currentUser?.id
}
