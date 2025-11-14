package `in`.xroden.flockr.features.auth.data

import android.util.Log
import `in`.xroden.flockr.features.auth.model.Profile
import io.github.jan.supabase.SupabaseClient
// import io.github.jan.supabase.gotrue.Auth // No longer needed
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "AuthRepository"

    init {
        Log.d(TAG, "AuthRepository initialized")
        // FIXED: Use supabase.auth
        Log.d(TAG, "Current user: ${supabase.auth.currentUserOrNull()?.id}")
    }

    // FIXED: Use supabase.auth
    val sessionFlow: Flow<SessionStatus> = supabase.auth.sessionStatus.onEach { status ->
        Log.d(TAG, "Session status emitted: ${status::class.simpleName}")
        when (status) {
            is SessionStatus.Authenticated -> Log.d(TAG, "User ID: ${status.session.user?.id}")
            is SessionStatus.NotAuthenticated -> Log.d(TAG, "Not authenticated - is sign out: ${status.isSignOut}")
            is SessionStatus.NetworkError -> Log.e(TAG, "Network error occurred")
            is SessionStatus.LoadingFromStorage -> Log.d(TAG, "Loading session from storage...")
        }
    }

    // FIXED: Removed duplicate sessionFlow definition

    val currentUser: UserInfo?
        // FIXED: Use supabase.auth
        get() = supabase.auth.currentUserOrNull()

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        Log.d(TAG, "signUp: start - email=$email, fullName=$fullName")
        return try {
            val signUpResult = supabase.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                }
            }

            // Log what we can about the result and current user
            Log.d(TAG, "signUp: supabase.auth.signUpWith returned: ${signUpResult?.toString()}")
            val newUserId = supabase.auth.currentUserOrNull()?.id
            Log.d(TAG, "signUp: currentUser after signUp: $newUserId")

            // Try to load profile immediately to surface DB-related errors (if any) and log outcome
            try {
                if (newUserId != null) {
                    val profile = supabase.from("profiles")
                        .select(Columns.ALL) {
                            filter { eq("id", newUserId) }
                        }
                        .decodeSingle<Profile>()

                    Log.d(TAG, "signUp: loaded profile after signup: ${profile?.fullName} (onboarded=${profile?.hasCompletedOnboarding})")
                } else {
                    Log.w(TAG, "signUp: no current user id after signup, skipping profile load")
                }
            } catch (dbEx: Exception) {
                // This is the place where DB errors (like 'database error saving new users') would appear.
                Log.e(TAG, "signUp: error loading/creating profile for new user (possible DB error)", dbEx)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "signUp: exception while signing up user (email=$email)", e)
            Result.failure(e)
        }
    }

    // FIXED: The entire signIn function was malformed.
    suspend fun signIn(email: String, password: String): Result<Unit> {
        Log.d(TAG, "signIn: start - email=$email")
        return try {
            val signInResult = supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "signIn: supabase.auth.signInWith returned: ${signInResult?.toString()}")
            val signedInUser = supabase.auth.currentUserOrNull()?.id
            Log.d(TAG, "signIn: currentUser after signIn: $signedInUser")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "signIn: exception while signing in (email=$email)", e)
            Result.failure(e)
        }
    }

    // FIXED: Removed all the floating code that was here.

    suspend fun signOut() {
        Log.d(TAG, "signOut: start")
        try {
            supabase.auth.signOut()
            Log.d(TAG, "signOut: success")
        } catch (e: Exception) {
            Log.e(TAG, "signOut: error during signOut", e)
            throw e
        }
    }

    suspend fun getProfile(): Profile? {
        return try {
            val userId = currentUser?.id
            Log.d(TAG, "Getting profile for user: $userId")
            if (userId == null) {
                Log.w(TAG, "No current user, cannot get profile")
                return null
            }

            val profile = supabase.from("profiles")
                .select(Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Profile>()

            // Added logging from the floating code
            Log.d(TAG, "Profile loaded: ${profile.fullName}, onboarded: ${profile.hasCompletedOnboarding}")
            profile
        } catch (e: Exception) {
            // Added logging from the floating code
            Log.e(TAG, "Error getting profile", e)
            null
        }
    }

    suspend fun updateProfile(fullName: String?, hasCompletedOnboarding: Boolean?): Result<Unit> {
        return try {
            val userId = currentUser?.id ?: return Result.failure(Exception("No user logged in"))

            Log.d(TAG, "updateProfile: userId=$userId, fullName=$fullName, hasCompletedOnboarding=$hasCompletedOnboarding")

            // Build update object based on what parameters are provided
            when {
                fullName != null && hasCompletedOnboarding != null -> {
                    @kotlinx.serialization.Serializable
                    data class ProfileUpdateBoth(
                        val full_name: String,
                        val has_completed_onboarding: Boolean
                    )
                    supabase.from("profiles")
                        .update(ProfileUpdateBoth(fullName, hasCompletedOnboarding)) {
                            filter { eq("id", userId) }
                        }
                }
                fullName != null -> {
                    @kotlinx.serialization.Serializable
                    data class ProfileUpdateName(val full_name: String)
                    supabase.from("profiles")
                        .update(ProfileUpdateName(fullName)) {
                            filter { eq("id", userId) }
                        }
                }
                hasCompletedOnboarding != null -> {
                    @kotlinx.serialization.Serializable
                    data class ProfileUpdateOnboarding(val has_completed_onboarding: Boolean)
                    supabase.from("profiles")
                        .update(ProfileUpdateOnboarding(hasCompletedOnboarding)) {
                            filter { eq("id", userId) }
                        }
                }
                else -> {
                    Log.w(TAG, "updateProfile: no updates provided")
                    return Result.success(Unit)
                }
            }

            Log.d(TAG, "updateProfile: success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile: error", e)
            Result.failure(e)
        }
    }
}