package `in`.xroden.flockr.features.auth.data

import `in`.xroden.flockr.features.auth.model.Profile

/**
 * Repository interface for authentication operations.
 * Enables easy mocking for unit tests.
 */
interface IAuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentProfile(): Result<Profile?>
    suspend fun updateProfile(
        fullName: String? = null,
        avatarUrl: String? = null,
        hasCompletedOnboarding: Boolean? = null
    ): Result<Unit>
    suspend fun isUserAuthenticated(): Boolean
    fun getCurrentUserId(): String?
}
