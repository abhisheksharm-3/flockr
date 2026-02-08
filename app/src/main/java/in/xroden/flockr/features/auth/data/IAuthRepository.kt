package `in`.xroden.flockr.features.auth.data

import `in`.xroden.flockr.features.auth.model.Profile
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

/** Repository interface for authentication operations. */
interface IAuthRepository {
    val currentUser: UserInfo?
    val sessionFlow: Flow<SessionStatus>

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun getProfile(): Result<Profile?>
    suspend fun getCurrentProfile(): Result<Profile?>
    suspend fun updateProfile(
        fullName: String? = null,
        avatarUrl: String? = null,
        hasCompletedOnboarding: Boolean? = null
    ): Result<Unit>
    suspend fun isUserAuthenticated(): Boolean
    fun getCurrentUserId(): String?
}
